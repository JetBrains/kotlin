/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.annotationsByClassId
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallElement

internal class SymbolLightLazyAnnotation private constructor(
    private val classId: ClassId,
    private val annotatedSymbolPointer: KtSymbolPointer<KtAnnotatedSymbol>,
    private val ktModule: KtModule,
    private val index: Int?,
    specialAnnotationApplication: KtAnnotationApplication?,
    owner: PsiModifierList,
) : SymbolLightAbstractAnnotation(owner) {
    init {
        require(index != null || specialAnnotationApplication != null)
    }

    private val fqName: FqName = classId.asSingleFqName()

    val annotationApplication: Lazy<KtAnnotationApplication> = specialAnnotationApplication?.let(::lazyOf) ?: lazyPub {
        withAnnotatedSymbol { ktAnnotatedSymbol ->
            val applications = ktAnnotatedSymbol.annotationsByClassId(classId)
            applications.find { it.index == index }
                ?: error("expected index: $index, actual indices: ${applications.map { it.index }}")
        }
    }

    constructor(
        classId: ClassId,
        annotatedSymbolPointer: KtSymbolPointer<KtAnnotatedSymbol>,
        ktModule: KtModule,
        index: Int,
        owner: PsiModifierList,
    ) : this(
        classId = classId,
        annotatedSymbolPointer = annotatedSymbolPointer,
        ktModule = ktModule,
        index = index,
        specialAnnotationApplication = null,
        owner = owner,
    )

    constructor(
        classId: ClassId,
        annotatedSymbolPointer: KtSymbolPointer<KtAnnotatedSymbol>,
        ktModule: KtModule,
        annotationApplication: KtAnnotationApplication,
        owner: PsiModifierList,
    ) : this(
        classId = classId,
        annotatedSymbolPointer = annotatedSymbolPointer,
        ktModule = ktModule,
        index = null,
        specialAnnotationApplication = annotationApplication,
        owner = owner,
    )

    inline fun <T> withAnnotatedSymbol(crossinline action: context(KtAnalysisSession) (KtAnnotatedSymbol) -> T): T =
        annotatedSymbolPointer.withSymbol(ktModule, action)

    override val kotlinOrigin: KtCallElement? get() = annotationApplication.value.psi

    override fun getQualifiedName(): String = fqName.asString()

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        SymbolLightLazyAnnotationParameterList(this, lazyPub { annotationApplication.value.arguments })
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightLazyAnnotation &&
            other.fqName == fqName &&
            other.index == index &&
            other.ktModule == ktModule &&
            other.parent == parent

    override fun hashCode(): Int = fqName.hashCode()
}
