/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.utils.exceptions.logErrorWithAttachment

internal class SymbolLightLazyAnnotation(
    val annotationsProvider: AnnotationsProvider,
    private val annotationApplication: AnnotationApplication,
    owner: PsiElement,
) : SymbolLightAbstractAnnotation(owner) {
    init {
        requireNotNull(annotationApplication.annotation.classId)
    }

    private val classId: ClassId get() = annotationApplication.annotation.classId!!

    private val fqName: FqName = classId.asSingleFqName()

    override fun createReferenceInformationProvider(): ReferenceInformationProvider = ReferenceInformationHolder(
        referenceName = classId.shortClassName.asString(),
    )

    val annotationApplicationWithArgumentsInfo: Lazy<AnnotationApplication> =
        annotationApplication.takeUnless(AnnotationApplication::isDumb)?.let(::lazyOf)
            ?: lazyPub {
                val applications = annotationsProvider[classId]
                applications.getOrNull(annotationApplication.relativeIndex) ?: run {
                    thisLogger().logErrorWithAttachment("Cannot find annotation application") {
                        withEntry("annotationApplication", annotationApplication) { it.toString() }
                        withEntry("applications", applications) { it.toString() }
                    }

                    annotationApplication
                }
            }

    override val kotlinOrigin: KtCallElement?
        get() = annotationApplication.annotation.sourcePsi

    override fun getQualifiedName(): String = fqName.asString()

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        symbolLightAnnotationParameterList {
            val annotationApplication = annotationApplicationWithArgumentsInfo.value
            annotationApplication.annotation.normalizedArguments()
        }
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightLazyAnnotation &&
            other.fqName == fqName &&
            other.annotationApplication.relativeIndex == annotationApplication.relativeIndex &&
            other.annotationApplication.annotation.classId == annotationApplication.annotation.classId &&
            other.annotationApplication.useSiteTarget == annotationApplication.useSiteTarget &&
            other.annotationsProvider == annotationsProvider &&
            other.parent == parent

    override fun hashCode(): Int = fqName.hashCode()
}