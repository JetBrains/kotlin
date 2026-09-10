/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.AnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class SymbolLightTypeParameter private constructor(
    parent: SymbolLightTypeParameterList,
    private val index: Int,
    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>,
    override val kotlinOrigin: KtTypeParameter?,
) : SymbolLightTypeParameterBase<SymbolLightTypeParameterList>(parent) {

    constructor(
        parent: SymbolLightTypeParameterList,
        index: Int,
        typeParameterSymbol: KaTypeParameterSymbol,
    ) : this(
        parent = parent,
        index = index,
        typeParameterSymbolPointer = typeParameterSymbol.createPointer(),
        kotlinOrigin = typeParameterSymbol.sourcePsiSafe(),
    )

    private val ktModule: KaModule get() = parent.ktModule

    private inline fun <T> withTypeParameterSymbol(crossinline action: context(KaSession) (KaTypeParameterSymbol) -> T): T =
        typeParameterSymbolPointer.withSymbol(ktModule, action)

    override fun copy(): PsiElement = copyTo(parent)

    internal fun copyTo(parent: SymbolLightTypeParameterList): SymbolLightTypeParameter = SymbolLightTypeParameter(
        parent,
        index,
        typeParameterSymbolPointer,
        kotlinOrigin,
    )

    private val _extendsList: PsiReferenceList by lazyPub {
        val listBuilder = KotlinSuperTypeListBuilder(
            this,
            kotlinOrigin = null,
            manager = manager,
            language = language,
            role = PsiReferenceList.Role.EXTENDS_LIST
        )

        withTypeParameterSymbol { typeParameterSymbol ->
            typeParameterSymbol.upperBounds
                .filter { type ->
                    when (type) {
                        is KaClassType -> type.classId != StandardClassIds.Any
                        is KaErrorType -> false
                        else -> true
                    }
                }
                .mapNotNull {
                    mapType(it, this@SymbolLightTypeParameter, KaTypeMappingMode.GENERIC_ARGUMENT)
                }
                .forEach { listBuilder.addReference(it) }
        }

        listBuilder
    }

    override fun getExtendsList(): PsiReferenceList = _extendsList

    private val _name: String by lazyPub {
        withTypeParameterSymbol { it.name.asString() }
    }

    override fun getName(): String = _name

    override fun getIndex(): Int = index

    private val annotationsBox: AnnotationsBox = GranularAnnotationsBox(
        annotationsProvider = SymbolAnnotationsProvider(ktModule, typeParameterSymbolPointer)
    )

    override fun getAnnotations(): Array<PsiAnnotation> = annotationsBox.annotationsArray(this)
    override fun findAnnotation(qualifiedName: String): PsiAnnotation? = annotationsBox.findAnnotation(this, qualifiedName)
    override fun getAnnotation(fqn: String): PsiAnnotation? = findAnnotation(fqn)
    override fun hasAnnotation(fqn: String): Boolean = annotationsBox.hasAnnotation(this, fqn)
    override fun getApplicableAnnotations(): Array<PsiAnnotation> = annotations

    override fun getNavigationElement(): PsiElement = kotlinOrigin ?: parent.navigationElement

    override fun getUseScope(): SearchScope = kotlinOrigin?.useScope ?: parent.useScope

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightTypeParameter || other.ktModule != ktModule || other.index != index) return false
        if (kotlinOrigin != null || other.kotlinOrigin != null) {
            return other.kotlinOrigin == kotlinOrigin
        }

        return compareSymbolPointers(typeParameterSymbolPointer, other.typeParameterSymbolPointer) &&
                other.parent == parent
    }

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: name.hashCode()

    override fun getText(): String? = kotlinOrigin?.text
    override fun getTextRange(): TextRange? = kotlinOrigin?.textRange
    override fun getTextOffset(): Int = kotlinOrigin?.startOffset ?: -1
    override fun getStartOffsetInParent(): Int = kotlinOrigin?.startOffsetInParent ?: -1

    override fun isValid(): Boolean = super.isValid() && kotlinOrigin?.isValid ?: typeParameterSymbolPointer.isValid(ktModule)
}
