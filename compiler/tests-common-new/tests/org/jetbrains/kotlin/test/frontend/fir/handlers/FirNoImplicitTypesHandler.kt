/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class FirNoImplicitTypesHandler(testServices: TestServices) : FirAnalysisHandler(testServices, failureDisablesNextSteps = true) {
    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        val visitor = Visitor()
        for (firFile in info.firFiles.values) {
            firFile.acceptChildren(visitor, firFile)
        }
        if (visitor.detectedImplicitTypesParents.isNotEmpty()) {
            assertions.fail {
                buildString {
                    val count = visitor.detectedImplicitTypesParents.size
                    if (count == 1) {
                        appendLine("One implicit type was found:")
                    } else {
                        appendLine("$count implicit types were found:")
                    }
                    val types = visitor.detectedImplicitTypesParents.joinToString(separator = "\n") {
                        "   - Type in ${it.render()}"
                    }
                    append(types)
                }
            }
        }
    }

    private inner class Visitor : FirDefaultVisitor<Unit, FirElement>() {
        val detectedImplicitTypesParents = mutableListOf<FirElement>()

        override fun visitElement(element: FirElement, data: FirElement) {
            element.acceptChildren(this, element)
        }

        override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: FirElement) {
            detectedImplicitTypesParents += data
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
