/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.js.backend.ast.*

object JsTestFunctionTransformer {
    fun generateTestFunctionCall(testFunctionContainers: List<TestFunctionContainer>): JsInvocation? {
        if (testFunctionContainers.isEmpty()) return null

        val testFunBody = JsBlock()
        val testFun = JsFunction(emptyScope, testFunBody, "root test fun")
        val suiteFunRef = testFunctionContainers.firstNotNullOf { it.suiteFunctionName }.makeRef()

        val tests = testFunctionContainers.groupBy({ it.packageFqn }) {
            JsInvocation(it.testFunctionName.makeRef()).makeStmt()
        } // String -> [IrSimpleFunction]

        for ((pkg, testCalls) in tests) {
            val pkgTestFun = JsFunction(emptyScope, JsBlock(), "test fun for $pkg")
            pkgTestFun.body.statements += testCalls
            testFun.body.statements += JsInvocation(suiteFunRef, JsStringLiteral(pkg), JsBooleanLiteral(false), pkgTestFun).makeStmt()
        }

        return JsInvocation(testFun)
    }

    class TestFunctionContainer(
        val packageFqn: String,
        val testFunctionName: JsName,
        val suiteFunctionName: JsName
    )
}

private fun Map<String, JsName>.getTestFunctionBySignature(signature: String?): JsName {
    return get(signature) ?: error("Null test functions should be filtered on a previous step")
}

private fun Map<String, JsName>.getSuiteFunctionBySignature(signature: String?): JsName {
    return get(signature) ?: error("A Suite function signature should be present if a test function signature does")
}

fun List<JsIrProgramFragment>.asTestFunctionContainers(): List<JsTestFunctionTransformer.TestFunctionContainer> {
    return mapNotNull { fragment ->
        fragment.testEnvironment?.let {
            JsTestFunctionTransformer.TestFunctionContainer(
                fragment.packageFqn,
                fragment.nameBindings.getTestFunctionBySignature(it.testFunctionTag),
                fragment.nameBindings.getSuiteFunctionBySignature(it.suiteFunctionTag)
            )
        }
    }
}

fun CachedTestFunctionsWithTheirPackage.asTestFunctionContainers(
    suiteFunction: String?,
    nameBindings: Map<String, JsName>
): List<JsTestFunctionTransformer.TestFunctionContainer> {
    return entries.flatMap { (packageFqn, testFunctions) ->
        testFunctions.map {
            JsTestFunctionTransformer.TestFunctionContainer(
                packageFqn,
                nameBindings.getTestFunctionBySignature(it),
                nameBindings.getSuiteFunctionBySignature(suiteFunction),
            )
        }
    }
}