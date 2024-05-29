/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.*
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.getBackReferences
import org.jetbrains.kotlin.bir.types.utils.isPrimitiveType
import org.jetbrains.kotlin.bir.types.utils.makeNullable
import org.jetbrains.kotlin.bir.util.render
import org.jetbrains.kotlin.bir.util.resolveFakeOverride

context(JvmBirBackendContext)
class BirJvmLateinitLowering : BirLoweringPhase() {
    private val lateinitProperties = registerIndexKey(BirProperty, false) {
        it.isLateinit && !it.isFakeOverride
    }
    private val lateinitVariables = registerIndexKey(BirVariable, false) {
        it.isLateinit
    }
    private val lateinitIsInitializedFunctionKey = registerIndexKey(BirSimpleFunction, true) {
        it == birBuiltIns.lateinitIsInitialized
    }
    private val variableReads = registerBackReferencesKey_valueSymbol(BirGetValue, BirGetValue::symbol)
    private val functionCalls = registerBackReferencesKey(BirCall, BirCall::symbol)

    override fun lower(module: BirModuleFragment) {
        transformLateinitProperties()
        transformLateinitVariables()
        transformIsLateinitInitialized()
    }

    private fun transformLateinitProperties() {
        getAllElementsWithIndex(lateinitProperties).forEach { property ->
            property.backingField!!.let {
                it.type = it.type.makeNullable()
            }
            transformLateinitPropertyGetter(property.getter!!, property.backingField!!)
        }
    }

    private fun transformLateinitVariables() {
        getAllElementsWithIndex(lateinitVariables).forEach { variable ->
            variable.type = variable.type.makeNullable()
            variable.isVar = true
            variable.initializer = BirConst.constNull(variable.sourceSpan, birBuiltIns.nothingNType)

            // todo: also transform reads of backing field?
            variable.getBackReferences(variableReads).forEach {
                transformGetLateinitVariable(it, variable)
            }

            variable.isLateinit = false
        }
    }

    private fun transformIsLateinitInitialized() {
        val lateinitIsInitializedFunction = getAllElementsWithIndex(lateinitIsInitializedFunctionKey).singleOrNull()
        lateinitIsInitializedFunction?.getBackReferences(functionCalls)?.forEach { call ->
            transformCallToLateinitIsInitializedPropertyGetter(call)
        }
    }

    private fun transformLateinitPropertyGetter(getter: BirFunction, backingField: BirField) {
        val type = backingField.type
        assert(!type.isPrimitiveType()) { "'lateinit' modifier is not allowed on primitive types" }
        birBodyScope {
            sourceSpan = getter.sourceSpan
            returnTarget = getter.symbol
            getter.body = birBlockBody {
                val resultVar = +birTemporaryVariable(
                    birGetField(
                        getter.dispatchReceiverParameter?.let { birGet(it) },
                        backingField,
                        backingField.type.makeNullable()
                    ),
                    addIndexToName = false,
                )
                +birIfThenElse(
                    birBuiltIns.nothingType,
                    birNotEquals(birGet(resultVar), birNull()),
                    birReturn(birGet(resultVar)),
                    throwUninitializedPropertyAccessException(backingField.name.asString())
                )
            }
        }
    }

    context(BirStatementBuilderScope)
    private fun throwUninitializedPropertyAccessException(name: String) = birBlock {
        +birCall(builtInSymbols.throwUninitializedPropertyAccessException.owner) {
            valueArguments[0] = birConst(name)
        }
    }

    private fun transformGetLateinitVariable(getVariable: BirGetValue, variable: BirVariable) {
        val ensureInitializedAndGet = birBodyScope {
            sourceSpan = getVariable.sourceSpan
            birIfThenElse(
                getVariable.type,
                birEquals(birGet(variable), birNull()),
                throwUninitializedPropertyAccessException(variable.name.asString()),
                birGet(variable)
            )
        }

        getVariable.replaceWith(ensureInitializedAndGet)
    }

    private fun transformCallToLateinitIsInitializedPropertyGetter(call: BirCall) {
        val notNullCheck = call.extensionReceiver!!.replaceTailExpression {
            val irPropertyRef = it as? BirPropertyReference
                ?: throw AssertionError("Property reference expected: ${it.render()}")
            val property = irPropertyRef.getter?.owner?.resolveFakeOverride()?.correspondingPropertySymbol?.owner
                ?: throw AssertionError("isInitialized cannot be invoked on ${it.render()}")
            require(property.isLateinit) {
                "isInitialized invoked on non-lateinit property ${property.render()}"
            }
            val backingField = property.backingField
                ?: throw AssertionError("Lateinit property is supposed to have a backing field")

            birBodyScope {
                sourceSpan = call.sourceSpan
                birNotEquals(birGetField(it.dispatchReceiver, backingField), birNull())
            }
        }

        call.replaceWith(notNullCheck)
    }

    private fun BirExpression.replaceTailExpression(transform: (BirExpression) -> BirExpression): BirExpression {
        var current = this
        var block: BirContainerExpression? = null
        while (current is BirContainerExpression) {
            block = current
            current = current.statements.last() as BirExpression
        }
        current = transform(current)
        if (block == null) {
            return current
        }
        block.statements[block.statements.lastIndex] = current
        return this
    }
}