/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.*
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.psi.*

public fun getJvmSignatureDiagnostics(element: PsiElement, otherDiagnostics: Diagnostics, moduleScope: GlobalSearchScope): Diagnostics? {
    fun getDiagnosticsForPackage(file: JetFile): Diagnostics? {
        val project = file.getProject()
        val cache = KotlinLightClassForFacade.FileStubCache.getInstance(project)
        return cache[file.getPackageFqName(), moduleScope].getValue()?.extraDiagnostics
    }

    fun getDiagnosticsForClass(jetClassOrObject: JetClassOrObject): Diagnostics {
        return KotlinLightClassForExplicitDeclaration.getLightClassData(jetClassOrObject).extraDiagnostics
    }

    fun doGetDiagnostics(): Diagnostics? {
        var parent = element.getParent()
        if (element is JetPropertyAccessor) {
            parent = parent?.getParent()
        }
        if (element is JetParameter && element.hasValOrVar()) {
            // property declared in constructor
            val parentClass = (parent?.getParent()?.getParent() as? JetClass)
            if (parentClass != null) {
                return getDiagnosticsForClass(parentClass)
            }
        }
        if (element is JetClassOrObject) {
            return getDiagnosticsForClass(element)
        }

        when (parent) {
            is JetFile -> {
                return getDiagnosticsForPackage(parent)
            }
            is JetClassBody -> {
                val parentsParent = parent.getParent()

                if (parentsParent is JetClassOrObject) {
                    return getDiagnosticsForClass(parentsParent)
                }
            }
        }
        return null
    }

    val result = doGetDiagnostics()
    if (result == null) return null

    return FilteredJvmDiagnostics(result, otherDiagnostics)
}

class FilteredJvmDiagnostics(val jvmDiagnostics: Diagnostics, val otherDiagnostics: Diagnostics) : Diagnostics by jvmDiagnostics {

    private fun alreadyReported(psiElement: PsiElement): Boolean {
        val higherPriority = setOf<DiagnosticFactory<*>>(
                CONFLICTING_OVERLOADS, REDECLARATION, NOTHING_TO_OVERRIDE, MANY_IMPL_MEMBER_NOT_IMPLEMENTED)
        return otherDiagnostics.forElement(psiElement).any { it.getFactory() in higherPriority }
                || psiElement is JetPropertyAccessor && alreadyReported(psiElement.getParent()!!)
    }

    override fun forElement(psiElement: PsiElement): Collection<Diagnostic> {
        val jvmDiagnosticFactories = setOf(CONFLICTING_JVM_DECLARATIONS, ACCIDENTAL_OVERRIDE)
        fun Diagnostic.data() = cast(this, jvmDiagnosticFactories).getA()
        val (conflicting, other) = jvmDiagnostics.forElement(psiElement).partition { it.getFactory() in jvmDiagnosticFactories }
        if (alreadyReported(psiElement)) {
            // CONFLICTING_OVERLOADS already reported, no need to duplicate it
            return other
        }

        val filtered = arrayListOf<Diagnostic>()
        conflicting.groupBy {
            it.data().signature.name
        }.forEach {
            val diagnostics = it.getValue()
            if (diagnostics.size() <= 1) {
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
                                                || other.data() higherThan me.data()
                                        )
                            }
                        }
                )
            }
        }

        return filtered + other
    }

    override fun all(): Collection<Diagnostic> {
        return jvmDiagnostics.all()
            .map { it.getPsiElement() }
            .toSet()
            .flatMap { forElement(it) }
    }
}

private fun ConflictingJvmDeclarationsData.higherThan(other: ConflictingJvmDeclarationsData): Boolean {
    return when (other.classOrigin.originKind) {
        PACKAGE_PART -> this.classOrigin.originKind == PACKAGE_FACADE
        TRAIT_IMPL -> this.classOrigin.originKind != TRAIT_IMPL
        else -> false
    }
}
