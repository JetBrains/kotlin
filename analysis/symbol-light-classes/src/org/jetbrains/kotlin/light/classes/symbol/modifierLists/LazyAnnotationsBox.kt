/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.annotationClassIds
import org.jetbrains.kotlin.analysis.api.annotations.annotationsByClassId
import org.jetbrains.kotlin.analysis.api.annotations.hasAnnotation
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightLazyAnnotation
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.SmartList
import java.util.concurrent.atomic.AtomicReference

internal class LazyAnnotationsBox(
    private val annotatedSymbolPointer: KtSymbolPointer<KtAnnotatedSymbol>,
    private val ktModule: KtModule,
    private val owner: PsiModifierList,
) : PsiAnnotationOwner {
    private inline fun <T> withAnnotatedSymbol(crossinline action: context(KtAnalysisSession) (KtAnnotatedSymbol) -> T): T =
        annotatedSymbolPointer.withSymbol(ktModule, action)

    private val annotationsArray: AtomicReference<Array<SymbolLightLazyAnnotation>?> = AtomicReference()
    private var specialAnnotations: SmartList<SymbolLightLazyAnnotation>? = null
    private val monitor = Any()

    override fun getAnnotations(): Array<SymbolLightLazyAnnotation> {
        annotationsArray.get()?.let { return it }
        val classIds = withAnnotatedSymbol { ktAnnotatedSymbol ->
            ktAnnotatedSymbol.annotationClassIds
        }

        val annotations = classIds.withIndex().map { (index, classId) ->
            SymbolLightLazyAnnotation(classId, annotatedSymbolPointer, ktModule, index, owner)
        }

        val array = if (annotations.isNotEmpty()) annotations.toTypedArray() else emptyArray

        val valueToReturn = if (array.isEmpty()) {
            setAnnotationsArray(array)
        } else {
            synchronized(monitor) {
                specialAnnotations?.forEach { lazyAnnotation ->
                    val index = array.indexOfFirst { it.classId == lazyAnnotation.classId }
                    array[index] = lazyAnnotation
                }

                setAnnotationsArray(array)
            }
        }

        return valueToReturn
    }

    private fun setAnnotationsArray(array: Array<SymbolLightLazyAnnotation>): Array<SymbolLightLazyAnnotation> =
        if (annotationsArray.compareAndSet(null, array)) {
            array
        } else {
            annotationsArray.get() ?: error("Unexpected state")
        }

    override fun getApplicableAnnotations(): Array<SymbolLightLazyAnnotation> = annotations

    override fun findAnnotation(qualifiedName: String): PsiAnnotation? {
        annotationsArray.get()?.let { array ->
            return array.find { it.qualifiedName == qualifiedName }
        }

        val specialAnnotationClassId = specialAnnotationsList[qualifiedName]
        return if (specialAnnotationClassId != null) {
            val annotationApplication = withAnnotatedSymbol { ktAnnotatedSymbol ->
                ktAnnotatedSymbol.annotationsByClassId(specialAnnotationClassId).firstOrNull()
            } ?: return null

            val lazyAnnotation = SymbolLightLazyAnnotation(
                specialAnnotationClassId,
                annotatedSymbolPointer,
                ktModule,
                annotationApplication,
                owner,
            )

            synchronized(monitor) {
                if (specialAnnotations != null) {
                    val specialAnnotations = specialAnnotations!!
                    val oldAnnotation = specialAnnotations.find { it.classId == lazyAnnotation.classId }
                    if (oldAnnotation != null) {
                        oldAnnotation
                    } else {
                        specialAnnotations += lazyAnnotation
                        lazyAnnotation
                    }
                } else {
                    specialAnnotations = SmartList(lazyAnnotation)
                    lazyAnnotation
                }
            }
        } else {
            annotations.find { it.qualifiedName == qualifiedName }
        }
    }

    override fun hasAnnotation(qualifiedName: String): Boolean {
        annotationsArray.get()?.let { array ->
            return array.any { it.qualifiedName == qualifiedName }
        }

        val specialAnnotationClassId = specialAnnotationsList[qualifiedName]
        return if (specialAnnotationClassId != null) {
            withAnnotatedSymbol { ktAnnotatedSymbol -> ktAnnotatedSymbol.hasAnnotation(specialAnnotationClassId) }
        } else {
            annotations.any { it.qualifiedName == qualifiedName }
        }
    }

    override fun addAnnotation(qualifiedName: String): PsiAnnotation = throw UnsupportedOperationException()

    companion object {
        /**
         * @see org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper
         */
        private val specialAnnotationsList: Map<String, ClassId> = listOf(
            StandardClassIds.Annotations.Deprecated,
//        Annotations.Retention,
//        Annotations.Target,
            StandardClassIds.Annotations.DeprecatedSinceKotlin,
            StandardClassIds.Annotations.WasExperimental,
            StandardClassIds.Annotations.JvmRecord,
//        Annotations.Repeatable,
//        Annotations.Java.Repeatable,
        ).associateBy { it.asFqNameString() }

        private val emptyArray: Array<SymbolLightLazyAnnotation> = emptyArray<SymbolLightLazyAnnotation>()
    }
}
