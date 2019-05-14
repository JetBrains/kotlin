/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.konan.descriptors.getArgumentValueOrNull
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


val IrDeclaration.name: Name
    get() = when (this) {
        is IrSimpleFunction -> this.name
        is IrClass -> this.name
        is IrEnumEntry -> this.name
        is IrProperty -> this.name
        is IrLocalDelegatedProperty -> this.name
        is IrField -> this.name
        is IrVariable -> this.name
        is IrConstructor -> SPECIAL_INIT_NAME
        is IrValueParameter -> this.name
        else -> error(this)
    }

private val SPECIAL_INIT_NAME = Name.special("<init>")

val IrField.fqNameForIrSerialization: FqName get() = this.parent.fqNameForIrSerialization.child(this.name)

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
val IrFunction.allParameters: List<IrValueParameter>
    get() = if (this is IrConstructor) {
        listOf(this.constructedClass.thisReceiver
                ?: error(this.descriptor)
        ) + explicitParameters
    } else {
        explicitParameters
    }

fun IrClass.isUnit() = this.fqNameForIrSerialization == KotlinBuiltIns.FQ_NAMES.unit.toSafe()

fun IrClass.isKotlinArray() = this.fqNameForIrSerialization == KotlinBuiltIns.FQ_NAMES.array.toSafe()

val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }
fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.fqNameForIrSerialization == KotlinBuiltIns.FQ_NAMES.any.toSafe()
fun IrClass.isNothing() = this.fqNameForIrSerialization == KotlinBuiltIns.FQ_NAMES.nothing.toSafe()

fun IrClass.getSuperInterfaces() = this.superClasses.map { it.owner }.filter { it.isInterface }

val IrProperty.konanBackingField: IrField?
    get() {
        assert(this.isReal)
        return this.backingField
    }

val IrFunction.isReal get() = this.origin != IrDeclarationOrigin.FAKE_OVERRIDE

// Note: psi2ir doesn't set `origin = FAKE_OVERRIDE` for fields and properties yet.
val IrProperty.isReal: Boolean get() = this.descriptor.kind.isReal
val IrField.isReal: Boolean get() = this.descriptor.kind.isReal

val IrSimpleFunction.isOverridable: Boolean
    get() = visibility != Visibilities.PRIVATE
            && modality != Modality.FINAL
            && (parent as? IrClass)?.isFinalClass != true

val IrFunction.isOverridable get() = this is IrSimpleFunction && this.isOverridable

val IrFunction.isOverridableOrOverrides
    get() = this is IrSimpleFunction && (this.isOverridable || this.overriddenSymbols.isNotEmpty())

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL

fun IrClass.isSpecialClassWithNoSupertypes() = this.isAny() || this.isNothing()

fun <T> IrDeclaration.getAnnotationArgumentValue(fqName: FqName, argumentName: String): T? {
    val annotation = this.annotations.findAnnotation(fqName)
    if (annotation == null) {
        // As a last resort try searching the descriptor.
        // This is needed for a period while we don't have IR for platform libraries.
        return this.descriptor.annotations
            .findAnnotation(fqName)
            ?.getArgumentValueOrNull<T>(argumentName)
    }
    for (index in 0 until annotation.valueArgumentsCount) {
        val parameter = annotation.symbol.owner.valueParameters[index]
        if (parameter.name == Name.identifier(argumentName)) {
            val actual = annotation.getValueArgument(index) as? IrConst<T>
            return actual?.value
        }
    }
    return null
}

fun IrValueParameter.isInlineParameter(): Boolean =
    !this.isNoinline && (this.type.isFunction() || this.type.isSuspendFunction()) && !this.type.isMarkedNullable()

val IrDeclaration.parentDeclarationsWithSelf: Sequence<IrDeclaration>
    get() = generateSequence(this, { it.parent as? IrDeclaration })

fun IrClass.companionObject() = this.declarations.filterIsInstance<IrClass>().atMostOne { it.isCompanion }

val IrDeclaration.isGetter get() = this is IrSimpleFunction && this == this.correspondingProperty?.getter

val IrDeclaration.isSetter get() = this is IrSimpleFunction && this == this.correspondingProperty?.setter

val IrDeclaration.isAccessor get() = this.isGetter || this.isSetter

fun buildSimpleAnnotation(irBuiltIns: IrBuiltIns, startOffset: Int, endOffset: Int,
                          annotationClass: IrClass, vararg args: String): IrConstructorCall {
    val constructor = annotationClass.constructors.single()
    return IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, constructor.returnType, constructor.symbol).apply {
        args.forEachIndexed { index, arg ->
            assert(constructor.valueParameters[index].type == irBuiltIns.stringType) {
                "String type expected but was ${constructor.valueParameters[index].type}"
            }
            putValueArgument(index, IrConstImpl.string(startOffset, endOffset, irBuiltIns.stringType, arg))
        }
    }
}