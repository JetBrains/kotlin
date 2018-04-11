/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.resolve.JsPlatform.builtIns
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.Variance

class JsIntrinsics(private val module: ModuleDescriptor, private val irBuiltIns: IrBuiltIns, symbolTable: SymbolTable) {

    private val stubBuilder = DeclarationStubGenerator(symbolTable, JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB)

    // Equality
    val jsEqeq = binOpBool("jsEqeq")
    val jsNotEq = binOpBool("jsNotEq")
    val jsEqeqeq = binOpBool("jsEqeqeq")
    val jsNotEqeq = binOpBool("jsNotEqeq")

    // Unary operations
    val jsNot = unOpBool("jsNot")

    // Binary operations
    val jsPlus = binOp("jsPlus")
    val jsMinus = binOp("jsMinus")
    val jsMult = binOp("jsMult")
    val jsDiv = binOp("jsDiv")
    val jsMod = binOp("jsMod")

    val jsObjectCreate: IrSimpleFunction = defineObjectCreateIntrinsic()

    // Helpers:

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

    private fun unOpBool(name: String) = irBuiltIns.run { defineOperator(name, bool, listOf(anyN, anyN)) }
    private fun binOpBool(name: String) = irBuiltIns.run { defineOperator(name, bool, listOf(anyN, anyN)) }
    private fun unOp(name: String) = irBuiltIns.run { defineOperator(name, anyN, listOf(anyN, anyN)) }
    private fun binOp(name: String) = irBuiltIns.run { defineOperator(name, anyN, listOf(anyN, anyN)) }
}
