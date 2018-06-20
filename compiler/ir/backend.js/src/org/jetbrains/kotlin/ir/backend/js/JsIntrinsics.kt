/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.utils.createValueParameter
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.js.resolve.JsPlatform.builtIns
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.Variance

class JsIntrinsics(
    private val module: ModuleDescriptor,
    private val irBuiltIns: IrBuiltIns,
    context: JsIrBackendContext
) {

    private val stubBuilder = DeclarationStubGenerator(context.symbolTable, JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB)

    // Equality operations:

    val jsEqeq = binOpBool("jsEqeq")
    val jsNotEq = binOpBool("jsNotEq")
    val jsEqeqeq = binOpBool("jsEqeqeq")
    val jsNotEqeq = binOpBool("jsNotEqeq")

    val jsGt = binOpBool("jsGt")
    val jsGtEq = binOpBool("jsGtEq")
    val jsLt = binOpBool("jsLt")
    val jsLtEq = binOpBool("jsLtEq")


    // Unary operations:

    val jsNot = unOpBool("jsNot")

    val jsUnaryPlus = unOp("jsUnaryPlus")
    val jsUnaryMinus = unOp("jsUnaryMinus")

    val jsPrefixInc = unOp("jsPrefixInc")
    val jsPostfixInc = unOp("jsPostfixInc")
    val jsPrefixDec = unOp("jsPrefixDec")
    val jsPostfixDec = unOp("jsPostfixDec")

    // Binary operations:

    val jsPlus = binOp("jsPlus")
    val jsMinus = binOp("jsMinus")
    val jsMult = binOp("jsMult")
    val jsDiv = binOp("jsDiv")
    val jsMod = binOp("jsMod")

    val jsAnd = binOp("jsAnd")
    val jsOr = binOp("jsOr")


    // Bit operations:

    val jsBitAnd = binOpInt("jsBitAnd")
    val jsBitOr = binOpInt("jsBitOr")
    val jsBitXor = binOpInt("jsBitXor")
    val jsBitNot = unOpInt("jsBitNot")

    val jsBitShiftR = binOpInt("jsBitShiftR")
    val jsBitShiftRU = binOpInt("jsBitShiftRU")
    val jsBitShiftL = binOpInt("jsBitShiftL")


    // KFunction operations:

    val jsName = unOp("kCallableName", irBuiltIns.string)
    val jsPropertyGet = binOp("kPropertyGet")
    val jsPropertySet = tripleOp("kPropertySet", irBuiltIns.unit)


    // Type checks:

    val jsInstanceOf = binOpBool("jsInstanceOf")
    val jsTypeOf = unOp("jsTypeOf", irBuiltIns.string)


    // Other:

    val jsObjectCreate = defineObjectCreateIntrinsic() // Object.create
    val jsSetJSField = defineSetJSPropertyIntrinsic() // till we don't have dynamic type we use intrinsic which sets a field with any name
    val jsToJsType = defineToJsType() // creates name reference to KotlinType
    val jsCode = context.getInternalFunctions("js").singleOrNull()?.let { context.symbolTable.referenceFunction(it) } // js("<code>")

    // Helpers:

    private fun defineToJsType(): IrSimpleFunction {
        val desc = SimpleFunctionDescriptorImpl.create(
            module,
            Annotations.EMPTY,
            Name.identifier("\$toJSType\$"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {

            val typeParameter = TypeParameterDescriptorImpl.createWithDefaultBound(
                this,
                Annotations.EMPTY,
                false,
                Variance.INVARIANT,
                Name.identifier("T"),
                0
            )
            initialize(null, null, listOf(typeParameter), emptyList(), builtIns.anyType, Modality.FINAL, Visibilities.PUBLIC)
        }

        return stubBuilder.generateFunctionStub(desc)
    }

    // TODO: unify how we create intrinsic symbols
    private fun defineObjectCreateIntrinsic(): IrSimpleFunction {

        val typeParam = TypeParameterDescriptorImpl.createWithDefaultBound(
            builtIns.any,
            Annotations.EMPTY,
            true,
            Variance.INVARIANT,
            Name.identifier("T"),
            0
        )

        val returnType = KotlinTypeFactory.simpleType(Annotations.EMPTY, typeParam.typeConstructor, emptyList(), false)

        val desc = SimpleFunctionDescriptorImpl.create(
            module,
            Annotations.EMPTY,
            Name.identifier("Object\$create"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {
            initialize(null, null, listOf(typeParam), emptyList(), returnType, Modality.FINAL, Visibilities.PUBLIC)
            isInline = true
        }

        return stubBuilder.generateFunctionStub(desc)
    }

    private fun defineSetJSPropertyIntrinsic(): IrSimpleFunction {
        val returnType = irBuiltIns.unit

        val desc = SimpleFunctionDescriptorImpl.create(
            module,
            Annotations.EMPTY,
            Name.identifier("\$setJSProperty\$"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {

            val parameterDescriptors = listOf("receiver", "fieldName", "fieldValue")
                .mapIndexed { i, name -> createValueParameter(this, i, name, irBuiltIns.any) }
            initialize(null, null, emptyList(), parameterDescriptors, returnType, Modality.FINAL, Visibilities.PUBLIC)
        }

        return stubBuilder.generateFunctionStub(desc)
    }

    private fun unOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN)) }

    private fun unOpBool(name: String) = unOp(name, irBuiltIns.bool)
    private fun unOpInt(name: String) = unOp(name, irBuiltIns.int)

    private fun binOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN, anyN)) }

    private fun tripleOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN, anyN, anyN)) }

    private fun binOpBool(name: String) = binOp(name, irBuiltIns.bool)
    private fun binOpInt(name: String) = binOp(name, irBuiltIns.int)
}
