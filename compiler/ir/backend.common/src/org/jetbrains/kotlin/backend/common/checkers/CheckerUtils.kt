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
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isNullableArray
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.ir.util.isAccessor
import org.jetbrains.kotlin.ir.util.isPublishedApi
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.KOTLINTEST_MODULE_NAME
import org.jetbrains.kotlin.library.KOTLIN_JS_STDLIB_NAME
import org.jetbrains.kotlin.library.KOTLIN_NATIVE_STDLIB_NAME
import org.jetbrains.kotlin.library.KOTLIN_WASM_STDLIB_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

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

private fun IrDeclarationWithVisibility.isVisibleAsInternal(file: IrFile): Boolean {
    val referencedDeclarationPackageFragment = getPackageFragment()
    val module = file.module
    if (referencedDeclarationPackageFragment.symbol is DescriptorlessExternalPackageFragmentSymbol) {
        // When compiling JS stdlib, intrinsic declarations are moved to a special module that doesn't have a descriptor.
        // This happens after deserialization but before executing any lowerings, including IR validating lowering
        // See MoveBodilessDeclarationsToSeparatePlaceLowering
        return module.name.asString() == "<$KOTLIN_JS_STDLIB_NAME>"
    }
    return module.descriptor.shouldSeeInternalsOf(referencedDeclarationPackageFragment.moduleDescriptor)
}

private fun IrDeclarationWithVisibility.isVisibleAsPrivate(file: IrFile): Boolean {
    // We're comparing file entries instead of files themselves because on JS
    // MoveBodilessDeclarationsToSeparatePlaceLowering performs shallow copying of IrFiles for some reason
    return file.fileEntry == fileOrNull?.fileEntry
}

/**
 * The set of declarations' fully qualified names references to which we don't want to check for visibility violations.
 *
 * FIXME: This is temporary hack until KT-70295 is fixed.
 */
private val FQ_NAMES_EXCLUDED_FROM_VISIBILITY_CHECKS: Set<FqName> = listOf(
    "kotlin.js.sharedBoxCreate",
    "kotlin.js.sharedBoxWrite",
    "kotlin.js.sharedBoxRead",
    "kotlin.wasm.internal.ClosureBoxBoolean",
    "kotlin.wasm.internal.ClosureBoxByte",
    "kotlin.wasm.internal.ClosureBoxShort",
    "kotlin.wasm.internal.ClosureBoxChar",
    "kotlin.wasm.internal.ClosureBoxInt",
    "kotlin.wasm.internal.ClosureBoxLong",
    "kotlin.wasm.internal.ClosureBoxFloat",
    "kotlin.wasm.internal.ClosureBoxDouble",
    "kotlin.wasm.internal.ClosureBoxAny",
).mapTo(hashSetOf(), ::FqName)

internal fun checkVisibility(
    referencedDeclarationSymbol: IrSymbol,
    reference: IrElement,
    context: CheckerContext,
) {
    if (FQ_NAMES_EXCLUDED_FROM_VISIBILITY_CHECKS.any(referencedDeclarationSymbol::hasEqualFqName)) {
        return
    }

    if (context.file.module.name in EXCLUDED_MODULE_NAMES) return

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
            -> referencedDeclaration.isVisibleAsInternal(context.file)

        is EffectiveVisibility.Local,
        is EffectiveVisibility.PrivateInClass,
        is EffectiveVisibility.PrivateInFile,
            -> referencedDeclaration.isVisibleAsPrivate(context.file)

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
    context: CheckerContext,
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

internal fun IrExpression.ensureTypeIs(expectedType: IrType, context: CheckerContext) {
    if (type != expectedType) {
        context.error(this, "unexpected type: expected ${expectedType.render()}, got ${type.render()}")
    }
}

internal fun IrElement.checkFunctionProperties(function: IrFunction, context: CheckerContext) {
    if (function is IrSimpleFunction) {
        val property = function.correspondingPropertySymbol?.owner
        if (property != null && property.getter != function && property.setter != function) {
            context.error(this, "Orphaned property getter/setter ${function.render()}")
        }
    }
}

internal fun IrElement.checkFunctionDispatchReceiver(function: IrFunction, context: CheckerContext) {
    if (function.dispatchReceiverParameter?.type is IrDynamicType) {
        context.error(this, "Dispatch receivers with 'dynamic' type are not allowed")
    }
}

internal fun IrSymbol.ensureBound(expression: IrExpression, context: CheckerContext) {
    if (!this.isBound && expression.type !is IrDynamicType) {
        context.error(expression, "Unbound symbol $this")
    }
}
