/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallElement

internal class SymbolLightLazyAnnotation(
    val annotationsProvider: AnnotationsProvider,
    private val annotationApplication: KtAnnotationApplication,
    owner: PsiElement,
) : SymbolLightAbstractAnnotation(owner) {
    init {
        requireNotNull(annotationApplication.classId)
    }

    private val classId: ClassId get() = annotationApplication.classId!!

    private val fqName: FqName = classId.asSingleFqName()

    override fun createReferenceInformationProvider(): ReferenceInformationProvider = ReferenceInformationHolder(
        referenceName = classId.shortClassName.asString(),
    )

    val annotationApplicationWithArgumentsInfo: Lazy<KtAnnotationApplicationWithArgumentsInfo> =
        (annotationApplication as? KtAnnotationApplicationWithArgumentsInfo)?.let(::lazyOf) ?: lazyPub {
            val applications = annotationsProvider[classId]
            applications.find { it.index == annotationApplication.index }
                ?: error("expected application: ${annotationApplication}, actual indices: ${applications.map { it.index }}")
        }

    override val kotlinOrigin: KtCallElement? get() = annotationApplicationWithArgumentsInfo.value.psi

    override fun getQualifiedName(): String = fqName.asString()

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        symbolLightAnnotationParameterList {
            annotationApplicationWithArgumentsInfo.value.normalizedArguments()
        }
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightLazyAnnotation &&
            other.fqName == fqName &&
            other.annotationApplication.classId == annotationApplication.classId &&
            other.annotationApplication.index == annotationApplication.index &&
            other.annotationApplication.useSiteTarget == annotationApplication.useSiteTarget &&
            other.annotationApplication.isCallWithArguments == annotationApplication.isCallWithArguments &&
            other.annotationsProvider == annotationsProvider &&
            other.parent == parent

    override fun hashCode(): Int = fqName.hashCode()
}