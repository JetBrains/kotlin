/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.erasedUpperBound

/**
 * Get the default null/0 value for the type.
 *
 * This handles unboxing of non-nullable inline class types to their underlying types and produces
 * a null/0 default value for the resulting type. When such unboxing takes place it ensures that
 * the value is not reboxed and reunboxed by the codegen by using the unsafeCoerceIntrinsic.
 */
fun IrType.defaultValue(startOffset: Int, endOffset: Int, context: JvmBackendContext): IrExpression {
    val classifier = this.classifierOrNull
    if (classifier is IrTypeParameterSymbol) {
        return classifier.owner.representativeUpperBound.defaultValue(startOffset, endOffset, context)
    }

    if (this !is IrSimpleType || this.isMarkedNullable() || classOrNull?.owner?.isSingleFieldValueClass != true)
        return IrConstImpl.defaultValueForType(startOffset, endOffset, this)

    val underlyingType = unboxInlineClass()
    val defaultValueForUnderlyingType = IrConstImpl.defaultValueForType(startOffset, endOffset, underlyingType)
    return IrCallImpl.fromSymbolOwner(startOffset, endOffset, this, context.ir.symbols.unsafeCoerceIntrinsic).also {
        it.typeArguments[0] = underlyingType
        it.typeArguments[1] = this
        it.arguments[0] = defaultValueForUnderlyingType
    }
}

fun IrType.isInlineClassType(): Boolean {
    // Workaround for KT-69856
    return if (this is IrSimpleType && classifier.owner is IrScript) {
        false
    } else {
        erasedUpperBound.isSingleFieldValueClass
    }
}

fun IrType.isMultiFieldValueClassType(): Boolean = erasedUpperBound.isMultiFieldValueClass

fun IrType.isValueClassType(): Boolean = erasedUpperBound.isValue

val IrType.upperBound: IrSimpleType
    get() = erasedUpperBound.symbol.starProjectedType

fun IrType.eraseToScope(scopeOwner: IrTypeParametersContainer): IrType =
    eraseToScope(collectVisibleTypeParameters(scopeOwner))

fun IrType.eraseToScope(visibleTypeParameters: Set<IrTypeParameter>): IrType {
    require(this is IrSimpleType) { error("Unexpected IrType kind: ${render()}") }
    return when (classifier) {
        is IrClassSymbol ->
            IrSimpleTypeImpl(
                classifier, nullability, arguments.map { it.eraseToScope(visibleTypeParameters) }, annotations
            )
        is IrTypeParameterSymbol ->
            if (classifier.owner in visibleTypeParameters)
                this
            else
                upperBound.mergeNullability(this)
        is IrScriptSymbol -> classifier.unexpectedSymbolKind<IrClassifierSymbol>()
    }
}

private fun IrTypeArgument.eraseToScope(visibleTypeParameters: Set<IrTypeParameter>): IrTypeArgument = when (this) {
    is IrStarProjection -> this
    is IrTypeProjection -> makeTypeProjection(type.eraseToScope(visibleTypeParameters), variance)
}

fun collectVisibleTypeParameters(scopeOwner: IrTypeParametersContainer): Set<IrTypeParameter> =
    generateSequence(scopeOwner) { current ->
        val parent = current.parent as? IrTypeParametersContainer
        parent.takeUnless { parent is IrClass && current is IrClass && !current.isInner && !current.isLocal }
    }
        .flatMap { it.typeParameters }
        .toSet()

val IrTypeParameter.representativeUpperBound: IrType
    get() {
        assert(superTypes.isNotEmpty()) { "Upper bounds should not be empty: ${render()}" }

        return superTypes.firstOrNull {
            val irClass = it.classOrNull?.owner ?: return@firstOrNull false
            irClass.kind != ClassKind.INTERFACE && irClass.kind != ClassKind.ANNOTATION_CLASS
        } ?: superTypes.first()
    }

@DeprecatedForRemovalCompilerApi(
    CompilerVersionOfApiDeprecation._2_1_20,
    "Moved to different package",
    "org.jetbrains.kotlin.ir.util.erasedUpperBound"
)
// generic parameter to deprioritize in overload resolution against moved one if both are star-imported
val <T : IrTypeParameter> T.erasedUpperBound
    get() = /*org.jetbrains.kotlin.ir.util.*/erasedUpperBound

@DeprecatedForRemovalCompilerApi(
    CompilerVersionOfApiDeprecation._2_1_20,
    "Moved to different package",
    "org.jetbrains.kotlin.ir.util.erasedUpperBound"
)
// generic parameter to deprioritize in overload resolution against moved one if both are star-imported
val <T : IrType> T.erasedUpperBound: IrClass
    get() = /*org.jetbrains.kotlin.ir.util.*/erasedUpperBound