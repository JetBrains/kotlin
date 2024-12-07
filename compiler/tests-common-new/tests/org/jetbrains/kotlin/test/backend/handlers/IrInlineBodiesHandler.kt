/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.lazy.AbstractIrLazyFunction
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.deserializedIr
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrFail
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class IrInlineBodiesHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    val declaredInlineFunctions = hashSetOf<IrSimpleFunction>()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun processModule(module: TestModule, info: IrBackendInput) {
        info.irModuleFragment.acceptChildrenVoid(InlineFunctionsCollector())
        info.irModuleFragment.acceptChildrenVoid(InlineCallBodiesCheck(firEnabled = module.frontendKind == FrontendKinds.FIR))

        assertions.assertTrue((info as IrBackendInput.JvmIrBackendInput).backendInput.symbolTable.descriptorExtension.allUnboundSymbols.isEmpty())
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertions.assertTrue(declaredInlineFunctions.isNotEmpty())
    }

    inner class InlineFunctionsCollector : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            if (declaration.isInline) declaredInlineFunctions.add(declaration)
            super.visitSimpleFunction(declaration)
        }
    }

    inner class InlineCallBodiesCheck(val firEnabled: Boolean) : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
            val symbol = expression.symbol
            assertions.assertTrue(symbol.isBound)
            val callee = symbol.owner
            if (callee is IrSimpleFunction && callee in declaredInlineFunctions) {
                val trueCallee = callee.resolveFakeOverrideOrFail()
                assertions.assertTrue(trueCallee.hasBody()) {
                    "IrInlineBodiesHandler: function with body expected"
                }
            }
            super.visitMemberAccess(expression)
        }

        private fun IrSimpleFunction.hasBody(): Boolean {
            if (this !is AbstractIrLazyFunction) return body != null
            if (!isDeserializationEnabled) return false
            if (!isInline || isFakeOverride) return false
            val topLevelDeclaration = getTopLevelDeclaration()
            if (topLevelDeclaration is IrClass && topLevelDeclaration.deserializedIr != null) return true
            return when (firEnabled) {
                // In compilation with FIR parents of external top-levels functions are replaced
                //   in lowerings, not in fir2ir converter
                true -> topLevelDeclaration.parent is IrExternalPackageFragment
                false -> false
            }
        }
    }
}
