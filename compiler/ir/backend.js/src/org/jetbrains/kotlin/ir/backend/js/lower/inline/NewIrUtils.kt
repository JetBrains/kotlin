/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.getTypeArgumentOrDefault
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

// backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/irasdescriptors/NewIrUtils.kt
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
                    override fun visitUByteValue(value: UByteValue?, data: Nothing?) {}
                    override fun visitUShortValue(value: UShortValue?, data: Nothing?) {}
                    override fun visitUIntValue(value: UIntValue?, data: Nothing?) {}
                    override fun visitULongValue(value: ULongValue?, data: Nothing?) {}
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