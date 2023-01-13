/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeAmbiguousSuper
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeFunctionExpectedError
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.IGNORE_LEAKED_INTERNAL_TYPES
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class FirResolvedTypesVerifier(testServices: TestServices) : FirAnalysisHandler(testServices, failureDisablesNextSteps = true) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        val visitor = Visitor()
        for (firFile in info.mainFirFiles.values) {
            firFile.acceptChildren(visitor, firFile)
        }
        val ignored = IGNORE_LEAKED_INTERNAL_TYPES in module.directives
        try {
            assertions.assertAll(
                { visitor.detectedImplicitTypesParents.check("implicit") },
                { visitor.detectedTypeVariableTypesParents.check("type variable") },
                { visitor.detectedStubTypesParents.check("stub") },
            )
        } catch (e: AssertionError) {
            if (ignored) {
                return
            } else {
                throw e
            }
        }
        if (ignored) {
            assertions.fail { "There is no leaked internal types in test. Please remove $IGNORE_LEAKED_INTERNAL_TYPES directive" }
        }
    }

    private fun Collection<FirElement>.check(typeName: String) {
        assertions.assertTrue(this.isEmpty()) {
            buildString {
                val count = size
                if (count == 1) {
                    appendLine("One $typeName type was found:")
                } else {
                    appendLine("$count $typeName types were found:")
                }
                val types = joinToString(separator = "\n") {
                    "   - Type in ${it.render()}"
                }
                append(types)
            }
        }
    }

    private inner class Visitor : FirDefaultVisitor<Unit, FirElement>() {
        val detectedImplicitTypesParents = mutableSetOf<FirElement>()
        val detectedTypeVariableTypesParents = mutableSetOf<FirElement>()
        val detectedStubTypesParents = mutableSetOf<FirElement>()

        override fun visitElement(element: FirElement, data: FirElement) {
            if (element is FirDiagnosticHolder) {
                for (coneType in element.diagnostic.coneTypes()) {
                    checkElementWithConeType(element, coneType)
                }
            }
            element.acceptChildren(this, element)
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: FirElement) {
            visitElement(resolvedTypeRef, data)
            checkElementWithConeType(resolvedTypeRef, resolvedTypeRef.type)
            resolvedTypeRef.delegatedTypeRef?.let { visitElement(it, data) }
        }

        override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: FirElement) {
            visitElement(errorTypeRef, data)
            errorTypeRef.delegatedTypeRef?.let { visitElement(it, data) }
            checkElementWithConeType(errorTypeRef, errorTypeRef.type)
        }

        override fun visitLoopJump(loopJump: FirLoopJump, data: FirElement) {
            visitElement(loopJump, data)
            if (loopJump.target.labeledElement is FirErrorLoop) {
                visitElement(loopJump.target.labeledElement, data)
            }
        }

        override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: FirElement) {
            detectedImplicitTypesParents += data
        }

        // --------------------------------------------------------------------------------------------

        private fun checkElementWithConeType(element: FirElement, type: ConeKotlinType) {
            when (checkConeType(type)) {
                ConeTypeStatus.TypeVariableFound -> detectedTypeVariableTypesParents += element
                ConeTypeStatus.StubFound -> detectedStubTypesParents += element
                null -> {}
            }
        }

        private fun checkConeType(type: ConeKotlinType): ConeTypeStatus? {
            var typeVariableFound = false
            var stubTypeFound = false
            type.contains {
                when (it) {
                    is ConeTypeVariableType -> typeVariableFound = true
                    is ConeStubType -> stubTypeFound = true
                    else -> {}
                }
                false
            }
            return when {
                stubTypeFound -> ConeTypeStatus.StubFound
                typeVariableFound -> ConeTypeStatus.TypeVariableFound
                else -> null
            }
        }

        private fun ConeDiagnostic.coneTypes(): List<ConeKotlinType> = when (this) {
            is ConeAmbiguousSuper -> candidateTypes
            is ConeFunctionExpectedError -> listOf(type)
            else -> emptyList()
        }
    }

    private enum class ConeTypeStatus {
        TypeVariableFound, StubFound
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
