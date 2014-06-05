/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.asJava

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.Diagnostics
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.resolve.java.diagnostics.ErrorsJvm.*
import org.jetbrains.jet.lang.resolve.java.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOriginKind.*
import org.jetbrains.jet.lang.diagnostics.Errors.*
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory.*

public fun getJvmSignatureDiagnostics(element: PsiElement, otherDiagnostics: Diagnostics): Diagnostics? {
    fun doGetDiagnostics(): Diagnostics? {
        var parent = element.getParent()
        if (element is JetPropertyAccessor) {
            parent = parent?.getParent()
        }

        if (parent is JetFile) {
            if (element is JetClassOrObject) {
                return getDiagnosticsForNonLocalClass(element)
            }
            return getDiagnosticsForPackage(parent as JetFile)
        }

        if (parent is JetClassBody) {
            val parentsParent = parent?.getParent()

            if (parentsParent is JetClassOrObject) {
                return getDiagnosticsForNonLocalClass(parentsParent)
            }
        }
        return null
    }

    val result = doGetDiagnostics()
    if (result == null) return null

    return object : Diagnostics by result {

        private fun alreadyReported(psiElement: PsiElement): Boolean {
            return otherDiagnostics.forElement(psiElement).any { it.getFactory() in setOf(CONFLICTING_OVERLOADS, REDECLARATION) }
                    || psiElement is JetPropertyAccessor && alreadyReported(psiElement.getParent()!!)
        }

        override fun forElement(psiElement: PsiElement): Collection<Diagnostic> {
            val jvmDiagnostics = setOf(CONFLICTING_JVM_DECLARATIONS, ACCIDENTAL_OVERRIDE)
            val (conflicting, other) = result.forElement(element).partition { it.getFactory() in jvmDiagnostics }
            if (alreadyReported(psiElement)) {
                // CONFLICTING_OVERLOADS already reported, no need to duplicate it
                return other
            }

            val filtered = arrayListOf<Diagnostic>()
            conflicting.groupBy {
                cast(it, jvmDiagnostics).getA().signature.name
            }.forEach {
                val diagnostics = it.getValue()
                if (diagnostics.size <= 1) {
                    filtered.addAll(diagnostics)
                }
                else {
                    filtered.addAll(
                            diagnostics.filter {
                                me ->
                                diagnostics.none {
                                    other ->
                                    me != other && (
                                            // in case of implementation copied from a super trait there will be both diagnostics on the same signature
                                            me.getFactory() == ACCIDENTAL_OVERRIDE && other.getFactory() == CONFLICTING_JVM_DECLARATIONS
                                               // there are paris of corresponding signatures that frequently clash simultaneously: package facade & part, trait and trait-impl
                                               || cast(other, jvmDiagnostics).getA() higherThan cast(me, jvmDiagnostics).getA()
                                            )
                                }
                            }
                    )
                }
            }

            return filtered + other
        }
    }
}

private fun ConflictingJvmDeclarationsData.higherThan(other: ConflictingJvmDeclarationsData): Boolean {
    return when (other.classOrigin.originKind) {
        PACKAGE_PART -> this.classOrigin.originKind == PACKAGE_FACADE
        TRAIT_IMPL -> this.classOrigin.originKind != TRAIT_IMPL
        else -> false
    }
}

private fun getDiagnosticsForPackage(file: JetFile): Diagnostics? {
    val project = file.getProject()
    val cache = KotlinLightClassForPackage.FileStubCache.getInstance(project)
    return cache[file.getPackageFqName(), GlobalSearchScope.allScope(project)].getValue()?.extraDiagnostics
}

private fun getDiagnosticsForNonLocalClass(jetClassOrObject: JetClassOrObject): Diagnostics {
    return KotlinLightClassForExplicitDeclaration.getLightClassData(jetClassOrObject).extraDiagnostics
}
