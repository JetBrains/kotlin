/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

val ANNOTATION_IMPLEMENTATION by IrDeclarationOriginImpl.Synthetic

/**
 * Creates synthetic annotations implementations and uses them in annotations constructor calls.
 *
 * For example:
 *
 *     annotation class A(val value: String)
 *     fun f(): A = A("")
 *
 * becomes
 *
 *     annotation class A(val value: String)
 *     fun f(): A = annotationImpl$A$0("")
 *
 *     class annotationImpl$A$0(override val value: String) : A {
 *         override fun equals(other: Any?): Boolean = ...
 *         override fun hashCode(): Int = ...
 *         override fun toString(): String = ...
 *         fun annotationType(): Class<*> = A::class.java // (JVM-only)
 *     }
 */
open class AnnotationImplementationLowering(
    val transformer: (IrFile) -> AnnotationImplementationTransformer
) : FileLoweringPass {
    override fun lower(irFile: IrFile) { TODO() }
}

abstract class AnnotationImplementationTransformer(val context: CommonBackendContext, val symbolTable: ReferenceSymbolTable, val irFile: IrFile?) :
    IrElementTransformerVoidWithContext() {
    internal val implementations: MutableMap<IrClass, IrClass> = mutableMapOf()


    override fun visitClassNew(declaration: IrClass): IrStatement { TODO() }

    protected fun IrClass.addConstructorBodyForCompatibility() { TODO() }

    abstract fun chooseConstructor(implClass: IrClass, expression: IrConstructorCall): IrConstructor

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression { TODO() }

    open fun IrClass.platformSetup() { TODO() }

    private fun moveValueArgumentsUsingNames(source: IrConstructorCall, destination: IrConstructorCall) { TODO() }

    private fun createAnnotationImplementation(annotationClass: IrClass): IrClass { TODO() }

    abstract fun implementAnnotationPropertiesAndConstructor(
        implClass: IrClass,
        annotationClass: IrClass,
        generatedConstructor: IrConstructor
    )

    fun IrClass.getAnnotationProperties(): List<IrProperty> { TODO() }

    abstract fun getArrayContentEqualsSymbol(type: IrType): IrFunctionSymbol

    open fun IrExpression.transformArrayEqualsArgument(type: IrType, irBuilder: IrBlockBodyBuilder): IrExpression = TODO()

    fun generatedEquals(irBuilder: IrBlockBodyBuilder, type: IrType, arg1: IrExpression, arg2: IrExpression): IrExpression = TODO()

    open val forbidDirectFieldAccessInMethods = false

    open fun generateFunctionBodies(
        annotationClass: IrClass,
        implClass: IrClass,
        eqFun: IrSimpleFunction,
        hcFun: IrSimpleFunction,
        toStringFun: IrSimpleFunction,
        generator: AnnotationImplementationMemberGenerator
    ) { TODO() }

    fun implementGeneratedFunctions(annotationClass: IrClass, implClass: IrClass) { TODO() }

    open fun implementPlatformSpecificParts(annotationClass: IrClass, implClass: IrClass) { TODO() }
}

class AnnotationImplementationMemberGenerator(
    loweringContext: LoweringContext,
    symbolTable: ReferenceSymbolTable,
    irClass: IrClass,
    val nameForToString: String,
    forbidDirectFieldAccess: Boolean,
    val selectEquals: IrBlockBodyBuilder.(IrType, IrExpression, IrExpression) -> IrExpression
) : LoweringDataClassMemberGenerator(loweringContext, symbolTable, irClass, ANNOTATION_IMPLEMENTATION, forbidDirectFieldAccess) {

    override fun IrClass.classNameForToString(): String = TODO()

    // From https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/Annotation.html#equals-java.lang.Object-
    // ---
    // The hash code of an annotation is the sum of the hash codes of its members (including those with default values), as defined below:
    // The hash code of an annotation member is (127 times the hash code of the member-name as computed by String.hashCode()) XOR the hash code of the member-value
    override fun IrBuilderWithScope.shiftResultOfHashCode(irResultVar: IrVariable): IrExpression = TODO()

    override fun getHashCodeOf(builder: IrBuilderWithScope, property: IrProperty, irValue: IrExpression): IrExpression = TODO()

    private fun IrBuilderWithScope.getHashCodeOf(type: IrType, irValue: IrExpression): IrExpression { TODO() }

    // Manual implementation of equals is required for following reasons:
    // 1. `other` should be casted to interface instead of implementation
    // 2. Properties should be retrieved using getters without accessing backing fields
    //    (DataClassMembersGenerator typically tries to access fields)
    // 3. Custom equals function should be used on properties
    fun generateEqualsUsingGetters(equalsFun: IrSimpleFunction, typeForEquals: IrType, properties: List<IrProperty>) = equalsFun.apply {
        body = loweringContext.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
            val irType = typeForEquals
            fun irOther() = irGet(parameters[1])
            fun irThis() = irGet(parameters[0])
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

