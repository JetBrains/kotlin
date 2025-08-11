/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.backend.common.checkers.expression.InlineFunctionUseSiteChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.toEffectiveVisibilityOrNull
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KOTLINTEST_MODULE_NAME
import org.jetbrains.kotlin.library.KOTLIN_JS_STDLIB_NAME
import org.jetbrains.kotlin.library.KOTLIN_NATIVE_STDLIB_NAME
import org.jetbrains.kotlin.library.KOTLIN_WASM_STDLIB_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

// FIXME: KT-78361 Investigate invalid IR in `kotlinx-serialization-core-js-1.7.0.klib` after enabling annotation validation
val EXCLUDED_PACKAGES_FROM_VARARG_VALIDATION = listOf(
    "kotlinx.serialization.modules",
    "kotlinx.serialization.json",
    "kotlinx.serialization.json.internal"
).mapTo(hashSetOf(), ::FqName)

context(checker: IrChecker)
internal fun validateVararg(irElement: IrElement, type: IrType, varargElementType: IrType, context: CheckerContext) {
    if (context.withinAnnotationUsageSubTree && context.file.packageFqName in EXCLUDED_PACKAGES_FROM_VARARG_VALIDATION) return

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

context(checker: IrChecker)
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
 * FIXME: We need to get rid of this list of exceptions (KT-69947).
 */
private val FQ_NAMES_EXCLUDED_FROM_VISIBILITY_CHECKS: Set<FqName> = listOf(
    "kotlin.wasm.internal.wasmTypeId",        // TODO: stop it leaking through kotlin.reflect.findAssociatedObject() inline function from Kotlin/Wasm stdlib, KT-76285
    "kotlin.coroutines.CoroutineImpl",        // TODO: stop it leaking through kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn() inline function in Kotlin/Wasm stdlib, KT-76285
).mapTo(hashSetOf(), ::FqName)

// Most of the internal annotations declared in these packages make visibility checks fail (KT-78100)
private val EXCLUDED_PACKAGES_FROM_ANNOTATIONS_VISIBILITY_CHECKS =
    listOf("kotlin.jvm", "kotlin.internal", "kotlin.native", "kotlin.native.internal").mapTo(hashSetOf(), ::FqName)

private fun IrSymbol.isExcludedFromVisibilityChecks(): Boolean {
    return FQ_NAMES_EXCLUDED_FROM_VISIBILITY_CHECKS.any { excludedFqName -> hasEqualFqName(excludedFqName) }
}

context(checker: IrChecker)
internal fun checkVisibility(
    referencedDeclarationSymbol: IrSymbol,
    reference: IrElement,
    context: CheckerContext,
) {
    if ((reference as? IrOverridableDeclaration<*>)?.isFakeOverride == true && referencedDeclarationSymbol in reference.overriddenSymbols) {
        return
    }

    if (reference is IrClass && referencedDeclarationSymbol in reference.sealedSubclasses) {
        return
    }

    if (referencedDeclarationSymbol.isExcludedFromVisibilityChecks()) {
        return
    }

    if (context.withinAnnotationUsageSubTree &&
        referencedDeclarationSymbol.owner.getPackageFragment()?.packageFqName in EXCLUDED_PACKAGES_FROM_ANNOTATIONS_VISIBILITY_CHECKS
    ) {
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

context(checker: IrChecker)
internal fun checkFunctionUseSite(
    expression: IrFunctionAccessExpression,
    inlineFunctionUseSiteChecker: InlineFunctionUseSiteChecker,
    context: CheckerContext,
) {
    if (!expression.symbol.isBound) return
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

context(checker: IrChecker)
internal fun IrExpression.ensureTypeIs(expectedType: IrType, context: CheckerContext) {
    if (type != expectedType) {
        context.error(this, "unexpected type: expected ${expectedType.render()}, got ${type.render()}")
    }
}