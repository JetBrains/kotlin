/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.inline.isCapturedFieldName
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class IrAnonymousObjectCapturedFieldsMatcher(
    private val codegen: ExpressionCodegen,
    private val candidateFiles: Set<IrFile>,
) {
    constructor(codegen: ExpressionCodegen, vararg candidates: IrFile?) : this(codegen, candidates.filterNotNull().toSet())

    fun resolveAnonymousObjectCapturedFieldsByConstructorArgument(ownerInternalName: String): List<String?>? {
        return findIrAnonymousObjectClassByInternalName(ownerInternalName)?.let(::getAnonymousObjectCapturedFieldsByConstructorArgumentFromIr)
    }

    private fun findIrAnonymousObjectClassByInternalName(ownerInternalName: String): IrClass? {
        for (file in candidateFiles) {
            findIrAnonymousObjectClassByInternalName(file, ownerInternalName)?.let { return it }
        }
        return null
    }

    private fun findIrAnonymousObjectClassByInternalName(file: IrFile, ownerInternalName: String): IrClass? {
        var result: IrClass? = null
        file.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement, data: Nothing?) {
                if (result != null) return
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (result != null) return
                if (declaration.isAnonymousObject &&
                    codegen.typeMapper.classLikeDeclarationInternalName(declaration) == ownerInternalName
                ) {
                    result = declaration
                    return
                }
                declaration.acceptChildrenVoid(this)
            }
        })
        return result
    }

    private fun getAnonymousObjectCapturedFieldsByConstructorArgumentFromIr(irClass: IrClass): List<String?>? {
        val constructor = irClass.constructors.singleOrNull() ?: return null
        val body = constructor.body as? IrBlockBody ?: return null
        val constructorArguments = constructor.nonDispatchParameters
        val valueParameterIndices = constructorArguments.withIndex().associate { indexedValue ->
            indexedValue.value.symbol to indexedValue.index
        }
        val capturedFieldsByConstructorArgument = MutableList<String?>(constructorArguments.size) { null }

        for (setField in body.collectStoreCapturedParameters()) {
            val fieldName = setField.symbol.owner.name.asString()
            val argumentIndex = valueParameterIndices[(setField.value as? IrGetValue)?.symbol] ?: continue
            capturedFieldsByConstructorArgument[argumentIndex] = fieldName
        }

        return capturedFieldsByConstructorArgument
    }

    private fun IrBlockBody.collectStoreCapturedParameters(): List<IrSetField> {
        val result = mutableListOf<IrSetField>()
        for (statement in statements) {
            collectStoreCapturedParameters(statement, result)
        }
        return result
    }

    private fun collectStoreCapturedParameters(statement: IrStatement, result: MutableList<IrSetField>) {
        when (statement) {
            is IrSetField -> {
                if (isCapturedFieldName(statement.symbol.owner.name.asString())) {
                    result += statement
                }
            }
            is IrContainerExpression -> {
                for (nestedStatement in statement.statements) {
                    collectStoreCapturedParameters(nestedStatement, result)
                }
            }
        }
    }
}
