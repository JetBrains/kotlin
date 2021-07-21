/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isKClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

val ANNOTATION_IMPLEMENTATION = object : IrDeclarationOriginImpl("ANNOTATION_IMPLEMENTATION", isSynthetic = true) {}

class AnnotationImplementationLowering(
    val transformer: (IrFile) -> AnnotationImplementationTransformer
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val tf = transformer(irFile)
        irFile.transformChildrenVoid(tf)
        tf.implementations.values.forEach {
            val parentClass = it.parent as IrDeclarationContainer
            parentClass.declarations += it
        }
    }
}

open class AnnotationImplementationTransformer(val context: BackendContext, val irFile: IrFile) : IrElementTransformerVoidWithContext() {
    internal val implementations: MutableMap<IrClass, IrClass> = mutableMapOf()

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructedClass = expression.type.classOrNull?.owner ?: return expression
        if (!constructedClass.isAnnotationClass) return expression
        if (constructedClass.typeParameters.isNotEmpty()) return expression // Not supported yet

        val implClass = implementations.getOrPut(constructedClass) { createAnnotationImplementation(constructedClass) }
        val ctor = implClass.constructors.single()
        val newCall = IrConstructorCallImpl.fromSymbolOwner(
            expression.startOffset,
            expression.endOffset,
            implClass.defaultType,
            ctor.symbol,
        )
        newCall.copyTypeAndValueArgumentsFrom(expression)
        newCall.transformChildrenVoid() // for annotations in annotations
        return newCall
    }

    private fun createAnnotationImplementation(annotationClass: IrClass): IrClass {
        val localDeclarationParent = currentClass?.scope?.getLocalDeclarationParent() as? IrClass
        val parentFqName = annotationClass.fqNameWhenAvailable!!.asString().replace('.', '_')
        val wrapperName = Name.identifier("annotationImpl\$$parentFqName$0")
        val subclass = context.irFactory.buildClass {
            name = wrapperName
            origin = ANNOTATION_IMPLEMENTATION
            // It can be seen from inline functions and multiple classes within one file
            // JavaDescriptorVisibilities.PACKAGE_VISIBILITY also can be used here, like in SAM, but that's not a big difference
            // since declaration is synthetic anyway
            visibility = DescriptorVisibilities.INTERNAL
        }.apply {
            parent = localDeclarationParent ?: irFile
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes = listOf(annotationClass.defaultType)
        }

        val ctor = subclass.addConstructor {
            visibility = DescriptorVisibilities.PUBLIC
        }
        val (originalProps, implementationProps) = implementAnnotationProperties(subclass, annotationClass, ctor)
        implementEqualsAndHashCode(annotationClass, subclass, originalProps, implementationProps)
        implementPlatformSpecificParts(annotationClass, subclass)
        return subclass
    }

    fun implementAnnotationProperties(implClass: IrClass, annotationClass: IrClass, generatedConstructor: IrConstructor): Pair<List<IrProperty>, List<IrProperty>> {
        val ctorBody = context.irFactory.createBlockBody(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                IrDelegatingConstructorCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, context.irBuiltIns.anyClass.constructors.single(),
                    typeArgumentsCount = 0, valueArgumentsCount = 0
                )
            )
        )

        generatedConstructor.body = ctorBody

        val properties = annotationClass.getAnnotationProperties()

        return properties to properties.map { property ->

            val propType = property.getter!!.returnType
            val propName = property.name
            val field = context.irFactory.buildField {
                name = propName
                type = propType
                origin = ANNOTATION_IMPLEMENTATION
                isFinal = true
                visibility = DescriptorVisibilities.PRIVATE
            }.also { it.parent = implClass }

            val parameter = generatedConstructor.addValueParameter(propName.asString(), propType)
            // VALUE_FROM_PARAMETER
            val originalParameter = ((property.backingField?.initializer?.expression as? IrGetValue)?.symbol?.owner as? IrValueParameter)
            if (originalParameter?.defaultValue != null) {
                parameter.defaultValue = originalParameter.defaultValue!!.deepCopyWithVariables().also { it.transformChildrenVoid() }
            }

            ctorBody.statements += IrSetFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol,
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, implClass.thisReceiver!!.symbol),
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.symbol),
                context.irBuiltIns.unitType,
            )

            val prop = implClass.addProperty {
                name = propName
                isVar = false
                origin = ANNOTATION_IMPLEMENTATION
            }.apply {
                backingField = field
                parent = implClass
            }

            prop.addGetter {
                name = propName  // Annotation value getter should be named 'x', not 'getX'
                returnType = propType.kClassToJClassIfNeeded() // On JVM, annotation store j.l.Class even if declared with KClass
                origin = ANNOTATION_IMPLEMENTATION
                visibility = DescriptorVisibilities.PUBLIC
                modality = Modality.FINAL
            }.apply {
                dispatchReceiverParameter = implClass.thisReceiver!!.copyTo(this)
                body = context.createIrBuilder(symbol).irBlockBody {
                    var value: IrExpression = irGetField(irGet(dispatchReceiverParameter!!), field)
                    if (propType.isKClass()) value = this.kClassExprToJClassIfNeeded(value)
                    +irReturn(value)
                }
            }

            prop
        }

    }

    fun IrClass.getAnnotationProperties(): List<IrProperty> {
        // For some weird reason, annotations defined in other IrFiles, do not have IrProperties in declarations.
        // (although annotations imported from Java do have)
        val props = declarations.filterIsInstance<IrProperty>()
        if (props.isNotEmpty()) return props
        return declarations.filterIsInstance<IrSimpleFunction>().filter { it.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR }
            .mapNotNull { it.correspondingPropertySymbol?.owner }
    }

    open fun IrType.kClassToJClassIfNeeded(): IrType = this

    open fun IrBuilderWithScope.kClassExprToJClassIfNeeded(irExpression: IrExpression): IrExpression = irExpression

    open fun generatedEquals(irBuilder: IrBlockBodyBuilder, type: IrType, arg1: IrExpression, arg2: IrExpression): IrExpression =
        irBuilder.irEquals(arg1, arg2)

    @Suppress("UNUSED_VARIABLE")
    fun implementEqualsAndHashCode(annotationClass: IrClass, implClass: IrClass, originalProps: List<IrProperty>, childProps: List<IrProperty>) {
        val creator = MethodsFromAnyGeneratorForLowerings(context, implClass, ANNOTATION_IMPLEMENTATION)
        val generator =
            creator.LoweringDataClassMemberGenerator(
                nameForToString = "@" + annotationClass.fqNameWhenAvailable!!.asString(),
                typeForEquals = annotationClass.defaultType
            ) { type, a, b ->
                generatedEquals(this, type, a, b)
            }

        // Manual implementation of equals is required for two reasons:
        // 1. `other` should be casted to interface instead of implementation
        // 2. Properties should be retrieved using getters without accessing backing fields
        //    (DataClassMembersGenerator typically tries to access fields)
        val eqFun = creator.createEqualsMethodDeclaration()
        generator.generateEqualsUsingGetters(eqFun, annotationClass.defaultType, originalProps)

        val hcFun = creator.createHashCodeMethodDeclaration()
        generator.generateHashCodeMethod(hcFun, childProps)

        val toStringFun = creator.createToStringMethodDeclaration()
        generator.generateToStringMethod(toStringFun, childProps)
    }

    open fun implementPlatformSpecificParts(annotationClass: IrClass, implClass: IrClass) {}
}

