/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.asJava.builder.InvalidLightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataProviderForFileFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory.cast
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ConflictingJvmDeclarationsData
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.*

fun getJvmSignatureDiagnostics(element: PsiElement, otherDiagnostics: Diagnostics, moduleScope: GlobalSearchScope): Diagnostics? {
    fun getDiagnosticsForFileFacade(file: KtFile): Diagnostics? {
        val project = file.project

        val facadeFqName = JvmFileClassUtil.getFileClassInfoNoResolve(file).facadeClassFqName

        //TODO Maybe it is better to cache this item
        return LightClassDataProviderForFileFacade.ByProjectSource(project, facadeFqName, moduleScope)
            .compute()
            ?.value
            ?.extraDiagnostics
    }

    fun getDiagnosticsForClass(ktClassOrObject: KtClassOrObject): Diagnostics {
        val lightClassDataHolder = KtLightClassForSourceDeclaration.getLightClassDataHolder(ktClassOrObject)
        if (lightClassDataHolder is InvalidLightClassDataHolder) {
            return Diagnostics.EMPTY
        }
        return lightClassDataHolder.extraDiagnostics
    }

    fun doGetDiagnostics(): Diagnostics? {
        //TODO: enable this diagnostic when light classes for scripts are ready
        if ((element.containingFile as? KtFile)?.isScript() == true) return null

        var parent = element.parent
        if (element is KtPropertyAccessor) {
            parent = parent?.parent
        }
        if (element is KtParameter && element.hasValOrVar()) {
            // property declared in constructor
            val parentClass = (parent?.parent?.parent as? KtClass)
            if (parentClass != null) {
                return getDiagnosticsForClass(parentClass)
            }
        }
        if (element is KtClassOrObject) {
            return getDiagnosticsForClass(element)
        }

        when (parent) {
            is KtFile -> {
                return getDiagnosticsForFileFacade(parent)
            }
            is KtClassBody -> {
                val parentsParent = parent.getParent()

                if (parentsParent is KtClassOrObject) {
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
        return otherDiagnostics.forElement(psiElement).any { it.factory in higherPriority }
                || psiElement is KtPropertyAccessor && alreadyReported(psiElement.parent)
    }

    override fun forElement(psiElement: PsiElement): Collection<Diagnostic> {
        val jvmDiagnosticFactories = setOf(CONFLICTING_JVM_DECLARATIONS, ACCIDENTAL_OVERRIDE, CONFLICTING_INHERITED_JVM_DECLARATIONS)
        fun Diagnostic.data() = cast(this, jvmDiagnosticFactories).a
        val (conflicting, other) = jvmDiagnostics.forElement(psiElement).partition { it.factory in jvmDiagnosticFactories }
        if (alreadyReported(psiElement)) {
            // CONFLICTING_OVERLOADS already reported, no need to duplicate it
            return other
        }

        val filtered = arrayListOf<Diagnostic>()
        conflicting.groupBy {
            it.data().signature.name
        }.forEach {
            val diagnostics = it.value
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
                                        other.factory == ErrorsJvm.CONFLICTING_JVM_DECLARATIONS && (me.factory == ACCIDENTAL_OVERRIDE ||
                                                                                                    me.factory == CONFLICTING_INHERITED_JVM_DECLARATIONS)
                                        // there are paris of corresponding signatures that frequently clash simultaneously: multifile class & part, trait and trait-impl
                                        || other.data().higherThan(me.data())
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
            .map { it.psiElement }
            .toSet()
            .flatMap { forElement(it) }
    }
}

private infix fun ConflictingJvmDeclarationsData.higherThan(other: ConflictingJvmDeclarationsData): Boolean {
    return when (other.classOrigin.originKind) {
        INTERFACE_DEFAULT_IMPL -> this.classOrigin.originKind != INTERFACE_DEFAULT_IMPL
        MULTIFILE_CLASS_PART -> this.classOrigin.originKind == MULTIFILE_CLASS
        else -> false
    }
}
