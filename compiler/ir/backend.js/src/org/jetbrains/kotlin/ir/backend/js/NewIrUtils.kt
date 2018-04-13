/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.getTypeArgumentOrDefault
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

val IrConstructor.constructedClass get() = this.parent as IrClass

val <T : IrDeclaration> T.original get() = this
val IrDeclaration.containingDeclaration get() = this.parent

val IrDeclarationParent.fqNameSafe: FqName
    get() = when (this) {
        is IrPackageFragment -> this.fqName
        is IrDeclaration -> this.parent.fqNameSafe.child(this.name)

        else -> error(this)
    }

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

val IrField.fqNameSafe: FqName get() = this.parent.fqNameSafe.child(this.name)

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

/**
 * @return naturally-ordered list of the parameters that can have values specified at call site.
 */
val IrFunction.explicitParameters: List<IrValueParameter>
    get() {
        val result = ArrayList<IrValueParameter>(valueParameters.size + 2)

        this.dispatchReceiverParameter?.let {
            result.add(it)
        }

        this.extensionReceiverParameter?.let {
            result.add(it)
        }

        result.addAll(valueParameters)

        return result
    }

val IrValueParameter.isVararg get() = this.varargElementType != null

val IrFunction.isSuspend get() = this is IrSimpleFunction && this.isSuspend

fun IrClass.isUnit() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.unit.toSafe()

//fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.any.toSafe()
fun IrClass.isNothing() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.nothing.toSafe()

//fun IrClass.getSuperInterfaces() = this.superClasses.map { it.owner }.filter { it.isInterface }

//val IrProperty.konanBackingField: IrField?
//    get() {
//        this.backingField?.let { return it }
//
//        (this.descriptor as? DeserializedPropertyDescriptor)?.backingField?.let { backingFieldDescriptor ->
//            val result = IrFieldImpl(
//                this.startOffset,
//                this.endOffset,
//                IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
//                backingFieldDescriptor
//            ).also {
//                it.parent = this.parent
//            }
//            this.backingField = result
//            return result
//        }
//
//        return null
//    }

val IrClass.defaultType: KotlinType
    get() = this.thisReceiver!!.type

val IrField.containingClass get() = this.parent as? IrClass

val IrFunction.isReal get() = this.origin != IrDeclarationOrigin.FAKE_OVERRIDE

val IrSimpleFunction.isOverridable: Boolean
    get() = visibility != Visibilities.PRIVATE
            && modality != Modality.FINAL
            && (parent as? IrClass)?.isFinalClass != true

val IrFunction.isOverridable get() = this is IrSimpleFunction && this.isOverridable

val IrFunction.isOverridableOrOverrides
    get() = this is IrSimpleFunction && (this.isOverridable || this.overriddenSymbols.isNotEmpty())

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS

fun IrSimpleFunction.overrides(other: IrSimpleFunction): Boolean {
    if (this == other) return true

    this.overriddenSymbols.forEach {
        if (it.owner.overrides(other)) {
            return true
        }
    }

    return false
}

fun IrClass.isSpecialClassWithNoSupertypes() = this.isAny() || this.isNothing()

val IrClass.constructors get() = this.declarations.filterIsInstance<IrConstructor>()

internal val IrValueParameter.isValueParameter get() = this.index >= 0

fun IrModuleFragment.referenceAllTypeExternalClassifiers(symbolTable: SymbolTable) {
    val moduleDescriptor = this.descriptor

    fun KotlinType.referenceAllClassifiers() {
        TypeUtils.getClassDescriptor(this)?.let {
            if (!ErrorUtils.isError(it) && it.module != moduleDescriptor) {
                if (it.kind == ClassKind.ENUM_ENTRY) {
                    symbolTable.referenceEnumEntry(it)
                } else {
                    symbolTable.referenceClass(it)
                }
            }
        }

        this.constructor.supertypes.forEach {
            it.referenceAllClassifiers()
        }
    }

    val visitor = object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitValueParameter(declaration: IrValueParameter) {
            super.visitValueParameter(declaration)
            declaration.type.referenceAllClassifiers()
        }

        override fun visitVariable(declaration: IrVariable) {
            super.visitVariable(declaration)
            declaration.type.referenceAllClassifiers()
        }

        override fun visitExpression(expression: IrExpression) {
            super.visitExpression(expression)
            expression.type.referenceAllClassifiers()
        }

        override fun visitDeclaration(declaration: IrDeclaration) {
            super.visitDeclaration(declaration)
            declaration.descriptor.annotations.getAllAnnotations().forEach {
                handleClassReferences(it.annotation)
            }
        }

        private fun handleClassReferences(annotation: AnnotationDescriptor) {
            annotation.allValueArguments.values.forEach {
                it.accept(object : AnnotationArgumentVisitor<Unit, Nothing?> {

                    override fun visitKClassValue(p0: KClassValue?, p1: Nothing?) {
                        p0?.value?.referenceAllClassifiers()
                    }

                    override fun visitArrayValue(p0: ArrayValue?, p1: Nothing?) {
                        p0?.value?.forEach { it.accept(this, null) }
                    }

                    override fun visitAnnotationValue(p0: AnnotationValue?, p1: Nothing?) {
                        p0?.let { handleClassReferences(p0.value) }
                    }

                    override fun visitBooleanValue(p0: BooleanValue?, p1: Nothing?) {}
                    override fun visitShortValue(p0: ShortValue?, p1: Nothing?) {}
                    override fun visitByteValue(p0: ByteValue?, p1: Nothing?) {}
                    override fun visitNullValue(p0: NullValue?, p1: Nothing?) {}
                    override fun visitDoubleValue(p0: DoubleValue?, p1: Nothing?) {}
                    override fun visitLongValue(p0: LongValue, p1: Nothing?) {}
                    override fun visitCharValue(p0: CharValue?, p1: Nothing?) {}
                    override fun visitIntValue(p0: IntValue?, p1: Nothing?) {}
                    override fun visitErrorValue(p0: ErrorValue?, p1: Nothing?) {}
                    override fun visitFloatValue(p0: FloatValue?, p1: Nothing?) {}
                    override fun visitEnumValue(p0: EnumValue?, p1: Nothing?) {}
                    override fun visitStringValue(p0: StringValue?, p1: Nothing?) {}
                }, null)
            }
        }

        override fun visitFunction(declaration: IrFunction) {
            super.visitFunction(declaration)
            declaration.returnType.referenceAllClassifiers()
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
            super.visitFunctionAccess(expression)
            expression.descriptor.original.typeParameters.forEach {
                expression.getTypeArgumentOrDefault(it).referenceAllClassifiers()
            }
        }
    }

    this.acceptVoid(visitor)
    this.dependencyModules.forEach { module ->
        module.externalPackageFragments.forEach {
            it.acceptVoid(visitor)
        }
    }
}