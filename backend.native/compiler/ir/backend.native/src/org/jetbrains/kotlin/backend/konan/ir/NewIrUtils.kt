/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
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
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

fun IrClass.isUnit() = this.fqNameForIrSerialization == StandardNames.FqNames.unit.toSafe()

fun IrClass.isKotlinArray() = this.fqNameForIrSerialization == StandardNames.FqNames.array.toSafe()

val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }
fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.fqNameForIrSerialization == StandardNames.FqNames.any.toSafe()
fun IrClass.isNothing() = this.fqNameForIrSerialization == StandardNames.FqNames.nothing.toSafe()

fun IrClass.getSuperInterfaces() = this.superClasses.map { it.owner }.filter { it.isInterface }

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

inline fun <reified T> IrDeclaration.getAnnotationArgumentValue(fqName: FqName, argumentName: String): T? {
    val annotation = this.annotations.findAnnotation(fqName) ?: return null
    for (index in 0 until annotation.valueArgumentsCount) {
        val parameter = annotation.symbol.owner.valueParameters[index]
        if (parameter.name == Name.identifier(argumentName)) {
            val actual = annotation.getValueArgument(index).safeAs<IrConst<T>>()
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

fun buildSimpleAnnotation(irBuiltIns: IrBuiltIns, startOffset: Int, endOffset: Int,
                          annotationClass: IrClass, vararg args: String): IrConstructorCall {
    val constructor = annotationClass.constructors.let {
        it.singleOrNull() ?: it.single { ctor -> ctor.valueParameters.size == args.size }
    }
    return IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, constructor.returnType, constructor.symbol).apply {
        args.forEachIndexed { index, arg ->
            assert(constructor.valueParameters[index].type == irBuiltIns.stringType) {
                "String type expected but was ${constructor.valueParameters[index].type}"
            }
            putValueArgument(index, IrConstImpl.string(startOffset, endOffset, irBuiltIns.stringType, arg))
        }
    }
}