/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.backend.common.InlineFunctionUseSiteChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.toEffectiveVisibilityOrNull
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isNullableArray
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isAccessor
import org.jetbrains.kotlin.ir.util.isPublishedApi
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.KOTLINTEST_MODULE_NAME
import org.jetbrains.kotlin.library.KOTLIN_JS_STDLIB_NAME
import org.jetbrains.kotlin.library.KOTLIN_NATIVE_STDLIB_NAME
import org.jetbrains.kotlin.library.KOTLIN_WASM_STDLIB_NAME
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.collections.get

internal fun validateVararg(irElement: IrElement, type: IrType, varargElementType: IrType, context: CheckerContext) {
    val isCorrectArrayOf = (type.isArray() || type.isNullableArray())
            && (type as IrSimpleType).arguments.single().let {
        when (it) {
            is IrSimpleType -> it == varargElementType
            is IrTypeProjection -> it.variance == Variance.OUT_VARIANCE && it.type == varargElementType
            else -> false
        }
    }
    if (isCorrectArrayOf) return

    val primitiveOrUnsignedElementType = type.classifierOrNull?.let { classifier ->
        context.irBuiltIns.primitiveArrayElementTypes[classifier]
            ?: context.irBuiltIns.unsignedArraysElementTypes[classifier]
    }
    val isCorrectArrayOfPrimitiveOrUnsigned = primitiveOrUnsignedElementType?.let { it == varargElementType }
    if (isCorrectArrayOfPrimitiveOrUnsigned == true) return

    context.error(
        irElement,
        "Vararg type=${type.render()} is expected to be an array of its underlying varargElementType=${varargElementType.render()}",
    )
}

internal val EXCLUDED_MODULE_NAMES: Set<Name> =
    arrayOf(
        KOTLIN_NATIVE_STDLIB_NAME,
        KOTLIN_JS_STDLIB_NAME,
        KOTLIN_WASM_STDLIB_NAME,
        KOTLINTEST_MODULE_NAME,
    ).mapTo(mutableSetOf()) { Name.special("<$it>") }

private fun visibilityError(element: IrElement, visibility: Visibility, context: CheckerContext) {
    val message = "The following element references " +
            if (visibility == Visibilities.Unknown) {
                "a declaration with unknown visibility:"
            } else {
                "'${visibility.name}' declaration that is invisible in the current scope:"
            }
    context.error(element, message)
}

private fun IrDeclarationWithVisibility.isVisibleAsInternal(context: CheckerContext): Boolean {
    val referencedDeclarationPackageFragment = getPackageFragment()
    val module = context.file.module
    if (referencedDeclarationPackageFragment.symbol is DescriptorlessExternalPackageFragmentSymbol) {
        // When compiling JS stdlib, intrinsic declarations are moved to a special module that doesn't have a descriptor.
        // This happens after deserialization but before executing any lowerings, including IR validating lowering
        // See MoveBodilessDeclarationsToSeparatePlaceLowering
        return module.name.asString() == "<$KOTLIN_JS_STDLIB_NAME>"
    }
    return module.descriptor.shouldSeeInternalsOf(referencedDeclarationPackageFragment.moduleDescriptor)
}

private fun IrDeclarationWithVisibility.isVisibleAsPrivate(context: CheckerContext): Boolean {
    // We're comparing file entries instead of files themselves because on JS
    // MoveBodilessDeclarationsToSeparatePlaceLowering performs shallow copying of IrFiles for some reason
    return context.file.fileEntry == fileOrNull?.fileEntry
}

internal fun checkVisibility(
    referencedDeclarationSymbol: IrSymbol,
    reference: IrElement,
    context: CheckerContext,
) {
    val referencedDeclaration = referencedDeclarationSymbol.owner as? IrDeclarationWithVisibility ?: return
    val classOfReferenced = referencedDeclaration.parentClassOrNull
    val visibility = referencedDeclaration.visibility.delegate

    val effectiveVisibility = visibility.toEffectiveVisibilityOrNull(
        container = classOfReferenced?.symbol,
        forClass = true,
        ownerIsPublishedApi = referencedDeclaration.isPublishedApi(),
    )

    val isVisible = when (effectiveVisibility) {
        is EffectiveVisibility.Internal,
        is EffectiveVisibility.InternalProtected,
        is EffectiveVisibility.InternalProtectedBound,
            -> referencedDeclaration.isVisibleAsInternal(context)

        is EffectiveVisibility.Local,
        is EffectiveVisibility.PrivateInClass,
        is EffectiveVisibility.PrivateInFile,
            -> referencedDeclaration.isVisibleAsPrivate(context)

        is EffectiveVisibility.PackagePrivate,
        is EffectiveVisibility.Protected,
        is EffectiveVisibility.ProtectedBound,
        is EffectiveVisibility.Public,
            -> true

        is EffectiveVisibility.Unknown, null -> false // We shouldn't encounter unknown visibilities at this point
    }

    if (!isVisible) {
        visibilityError(reference, visibility, context)
    }
}

internal fun checkFunctionUseSite(
    expression: IrMemberAccessExpression<IrFunctionSymbol>,
    inlineFunctionUseSiteChecker: InlineFunctionUseSiteChecker,
    context: CheckerContext
) {
    val function = expression.symbol.owner
    if (!function.isInline || inlineFunctionUseSiteChecker.isPermitted(expression)) return
    val message = buildString {
        append("The following element references ").append(function.visibility).append(" inline ")
        append(
            when (function) {
                is IrSimpleFunction -> if (function.isAccessor) "property accessor" else "function"
                is IrConstructor -> "constructor"
            }
        )
        append(" ").append(function.name.asString())
    }
    context.error(expression, message)
}