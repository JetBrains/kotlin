/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiModifierList
import com.intellij.psi.impl.PsiImplUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

internal class SymbolLightLazyAnnotation private constructor(
    classId: ClassId,
    private val annotatedSymbolPointer: KtSymbolPointer<KtAnnotatedSymbol>,
    private val ktModule: KtModule,
    private val index: Int?,
    specialAnnotationApplication: KtAnnotationApplication?,
    owner: PsiModifierList,
) : SymbolLightAbstractAnnotationWithClassId(classId, owner) {
    init {
        require(index != null || specialAnnotationApplication != null)
    }

    private val annotationApplication: Lazy<KtAnnotationApplication> = specialAnnotationApplication?.let(::lazyOf) ?: lazyPub {
        withAnnotatedSymbol { ktAnnotatedSymbol ->
            ktAnnotatedSymbol.annotations[index!!]
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

    private inline fun <T> withAnnotatedSymbol(crossinline action: context(KtAnalysisSession) (KtAnnotatedSymbol) -> T): T =
        annotatedSymbolPointer.withSymbol(ktModule, action)

    override val kotlinOrigin: KtCallElement? get() = annotationApplication.value.psi

    override fun getQualifiedName(): String = classId.asFqNameString()

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        SymbolLightLazyAnnotationParameterList(this, lazyPub { annotationApplication.value.arguments })
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList

    override fun findAttributeValue(attributeName: String?): PsiAnnotationMemberValue? =
        PsiImplUtil.findAttributeValue(this, attributeName)

    override fun findDeclaredAttributeValue(attributeName: String?) =
        PsiImplUtil.findDeclaredAttributeValue(this, attributeName)

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightLazyAnnotation &&
            other.classId == classId &&
            other.index == index &&
            other.ktModule == ktModule &&
            other.parent == parent

    override fun hashCode(): Int = classId.hashCode()
}
