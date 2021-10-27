/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addFakeOverrides
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isArray
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

abstract class AnnotationImplementationTransformer(val context: BackendContext, val irFile: IrFile?) : IrElementTransformerVoidWithContext() {
    internal val implementations: MutableMap<IrClass, IrClass> = mutableMapOf()


    override fun visitClassNew(declaration: IrClass): IrStatement {
        declaration.takeIf { declaration.isAnnotationClass }?.constructors?.singleOrNull()?.apply {
            // Compatibility hack. Now, frontend generates constructor body for annotations and makes them open
            // but, if one gets annotation from pre-1.6.20 klib, it would have no constructor body and would be final,
            // so we need to fix it
            if (body == null) {
                declaration.modality = Modality.OPEN
                body = context.createIrBuilder(symbol)
                    .irBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET) {
                        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, declaration.symbol, context.irBuiltIns.unitType)
                    }
            }
        }
        return super.visitClassNew(declaration)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructedClass = expression.type.classOrNull?.owner ?: return super.visitConstructorCall(expression)
        if (!constructedClass.isAnnotationClass) return super.visitConstructorCall(expression)
        if (constructedClass.typeParameters.isNotEmpty()) return super.visitConstructorCall(expression) // Not supported yet

        val implClass = implementations.getOrPut(constructedClass) { createAnnotationImplementation(constructedClass) }
        val ctor = implClass.constructors.single()
        val newCall = IrConstructorCallImpl.fromSymbolOwner(
            expression.startOffset,
            expression.endOffset,
            implClass.defaultType,
            ctor.symbol,
        )
        moveValueArgumentsUsingNames(expression, newCall)
        newCall.transformChildrenVoid() // for annotations in annotations
        return newCall
    }

    open fun IrClass.platformSetup() {}

    private fun moveValueArgumentsUsingNames(source: IrConstructorCall, destination: IrConstructorCall) {
        val argumentsByName = source.getArgumentsWithIr().associateBy(
            { (param, _) -> param.name },
            { (_, value) -> value }
        )

        destination.symbol.owner.valueParameters.forEachIndexed { index, parameter ->
            val valueArg = argumentsByName[parameter.name]
            if (parameter.defaultValue == null && valueArg == null)
                error("Usage of default value argument for this annotation is not yet possible.\n" +
                       "Please specify value for '${source.type.classOrNull?.owner?.name}.${parameter.name}' explicitly")
            destination.putValueArgument(index, valueArg)
        }
    }

    private fun createAnnotationImplementation(annotationClass: IrClass): IrClass {
        val localDeclarationParent = currentClass?.scope?.getLocalDeclarationParent() as? IrClass
        val parentFqName = annotationClass.fqNameWhenAvailable!!.asString().replace('.', '_')
        val wrapperName = Name.identifier("annotationImpl\$$parentFqName$0")
        val subclass = context.irFactory.buildClass {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = wrapperName
            origin = ANNOTATION_IMPLEMENTATION
            // It can be seen from inline functions and multiple classes within one file
            // JavaDescriptorVisibilities.PACKAGE_VISIBILITY also can be used here, like in SAM, but that's not a big difference
            // since declaration is synthetic anyway
            visibility = DescriptorVisibilities.INTERNAL
        }.apply {
            parent = localDeclarationParent ?: irFile
                    ?: error("irFile in transformer should be specified when creating synthetic implementation")
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes = listOf(annotationClass.defaultType)
            platformSetup()
        }

        val ctor = subclass.addConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            visibility = DescriptorVisibilities.PUBLIC
        }
        implementAnnotationPropertiesAndConstructor(subclass, annotationClass, ctor)
        implementGeneratedFunctions(annotationClass, subclass)
        implementPlatformSpecificParts(annotationClass, subclass)
        return subclass
    }

    abstract fun implementAnnotationPropertiesAndConstructor(
        implClass: IrClass,
        annotationClass: IrClass,
        generatedConstructor: IrConstructor
    )

    fun IrClass.getAnnotationProperties(): List<IrProperty> {
        // For some weird reason, annotations defined in other IrFiles, do not have IrProperties in declarations.
        // (although annotations imported from Java do have)
        val props = declarations.filterIsInstance<IrProperty>()
        if (props.isNotEmpty()) return props
        return declarations.filterIsInstance<IrSimpleFunction>().filter { it.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR }
            .mapNotNull { it.correspondingPropertySymbol?.owner }
    }

    open fun IrBuilderWithScope.kClassExprToJClassIfNeeded(irExpression: IrExpression): IrExpression = irExpression

    abstract fun getArrayContentEqualsSymbol(type: IrType): IrFunctionSymbol

    fun generatedEquals(irBuilder: IrBlockBodyBuilder, type: IrType, arg1: IrExpression, arg2: IrExpression): IrExpression =
        if (type.isArray() || type.isPrimitiveArray()) {
            val requiredSymbol = getArrayContentEqualsSymbol(type)
            irBuilder.irCall(
                requiredSymbol
            ).apply {
                if (requiredSymbol.owner.extensionReceiverParameter != null) {
                    extensionReceiver = arg1
                    putValueArgument(0, arg2)
                } else {
                    putValueArgument(0, arg1)
                    putValueArgument(1, arg2)
                }
            }
        } else
            irBuilder.irEquals(arg1, arg2)

    open val forbidDirectFieldAccessInMethods = false

    open fun generateFunctionBodies(
        annotationClass: IrClass,
        implClass: IrClass,
        eqFun: IrSimpleFunction,
        hcFun: IrSimpleFunction,
        toStringFun: IrSimpleFunction,
        generator: AnnotationImplementationMemberGenerator
    ) {
        val properties = annotationClass.getAnnotationProperties()
        generator.generateEqualsUsingGetters(eqFun, annotationClass.defaultType, properties)
        generator.generateHashCodeMethod(hcFun, properties)
        generator.generateToStringMethod(toStringFun, properties)
    }

    fun implementGeneratedFunctions(annotationClass: IrClass, implClass: IrClass) {
        val creator = MethodsFromAnyGeneratorForLowerings(context, implClass, ANNOTATION_IMPLEMENTATION)
        val eqFun = creator.createEqualsMethodDeclaration()
        val hcFun = creator.createHashCodeMethodDeclaration()
        val toStringFun = creator.createToStringMethodDeclaration()
        if (annotationClass != implClass) {
            implClass.addFakeOverrides(context.typeSystem)
        }

        val generator = AnnotationImplementationMemberGenerator(
            context, implClass,
            nameForToString = "@" + annotationClass.fqNameWhenAvailable!!.asString(),
            forbidDirectFieldAccess = forbidDirectFieldAccessInMethods
        ) { type, a, b ->
            generatedEquals(this, type, a, b)
        }

        generateFunctionBodies(annotationClass, implClass, eqFun, hcFun, toStringFun, generator)
    }

    open fun implementPlatformSpecificParts(annotationClass: IrClass, implClass: IrClass) {}
}

class AnnotationImplementationMemberGenerator(
    backendContext: BackendContext,
    irClass: IrClass,
    val nameForToString: String,
    forbidDirectFieldAccess: Boolean,
    val selectEquals: IrBlockBodyBuilder.(IrType, IrExpression, IrExpression) -> IrExpression,
) : LoweringDataClassMemberGenerator(backendContext, irClass, ANNOTATION_IMPLEMENTATION, forbidDirectFieldAccess) {

    override fun IrClass.classNameForToString(): String = nameForToString

    // From https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/Annotation.html#equals-java.lang.Object-
    // ---
    // The hash code of an annotation is the sum of the hash codes of its members (including those with default values), as defined below:
    // The hash code of an annotation member is (127 times the hash code of the member-name as computed by String.hashCode()) XOR the hash code of the member-value
    override fun IrBuilderWithScope.shiftResultOfHashCode(irResultVar: IrVariable): IrExpression = irGet(irResultVar) // no default (* 31)

    override fun getHashCodeOf(builder: IrBuilderWithScope, property: IrProperty, irValue: IrExpression): IrExpression = with(builder) {
        val propertyValueHashCode = getHashCodeOf(property.type, irValue)
        val propertyNameHashCode = getHashCodeOf(backendContext.irBuiltIns.stringType, irString(property.name.toString()))
        val multiplied = irCallOp(context.irBuiltIns.intTimesSymbol, context.irBuiltIns.intType, propertyNameHashCode, irInt(127))
        return irCallOp(context.irBuiltIns.intXorSymbol, context.irBuiltIns.intType, multiplied, propertyValueHashCode)
    }

    // Manual implementation of equals is required for following reasons:
    // 1. `other` should be casted to interface instead of implementation
    // 2. Properties should be retrieved using getters without accessing backing fields
    //    (DataClassMembersGenerator typically tries to access fields)
    // 3. Custom equals function should be used on properties
    fun generateEqualsUsingGetters(equalsFun: IrSimpleFunction, typeForEquals: IrType, properties: List<IrProperty>) = equalsFun.apply {
        body = backendContext.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            val irType = typeForEquals
            fun irOther() = irGet(valueParameters[0])
            fun irThis() = irGet(dispatchReceiverParameter!!)
            fun IrProperty.get(receiver: IrExpression) = irCall(getter!!).apply {
                dispatchReceiver = receiver
            }

            +irIfThenReturnFalse(irNotIs(irOther(), irType))
            val otherWithCast = irTemporary(irAs(irOther(), irType), "other_with_cast")
            for (property in properties) {
                val arg1 = property.get(irThis())
                val arg2 = property.get(irGet(irType, otherWithCast.symbol))
                +irIfThenReturnFalse(irNot(selectEquals(property.type, arg1, arg2)))
            }
            +irReturnTrue()
        }
    }
}
