/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodForMappedJavaCollectionStubMethod
import org.jetbrains.kotlin.psi.KtTypeParameter

/**
 * A type parameter of [SymbolLightMethodForMappedJavaCollectionStubMethod].
 *
 * It mirrors [javaTypeParameter] of the overridden Java method (name and bounds), but the stub method is its owner.
 * The bounds are substituted by the stub method, so they refer to the type arguments of the Kotlin class
 * and to the type parameters of the stub method rather than to the Java declaration.
 */
internal class SymbolLightTypeParameterForMappedJavaCollectionStubMethod(
    private val javaTypeParameter: PsiTypeParameter,
    parent: SymbolLightTypeParameterListForMappedJavaCollectionStubMethod,
    private val index: Int,
) : SymbolLightTypeParameterBase<SymbolLightTypeParameterListForMappedJavaCollectionStubMethod>(parent) {
    override val kotlinOrigin: KtTypeParameter? = null

    private val _name: String = javaTypeParameter.name ?: error("A type parameter without a name: $javaTypeParameter")

    override fun getName(): String = _name

    override fun getIndex(): Int = index

    private val _extendsList: PsiReferenceList by lazyPub {
        val listBuilder = KotlinSuperTypeListBuilder(
            this,
            kotlinOrigin = null,
            manager = manager,
            language = language,
            role = PsiReferenceList.Role.EXTENDS_LIST,
        )

        val substitutor = parent.owner.methodSubstitutor
        for (bound in javaTypeParameter.extendsListTypes) {
            listBuilder.addReference(substitutor.substitute(bound) as? PsiClassType ?: bound)
        }

        listBuilder
    }

    override fun getExtendsList(): PsiReferenceList = _extendsList

    override fun getAnnotations(): Array<PsiAnnotation> = PsiAnnotation.EMPTY_ARRAY
    override fun findAnnotation(qualifiedName: String): PsiAnnotation? = null
    override fun getAnnotation(fqn: String): PsiAnnotation? = null
    override fun hasAnnotation(fqn: String): Boolean = false
    override fun getApplicableAnnotations(): Array<PsiAnnotation> = PsiAnnotation.EMPTY_ARRAY

    override fun getNavigationElement(): PsiElement = javaTypeParameter.navigationElement

    override fun getUseScope(): SearchScope = parent.useScope

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightTypeParameterForMappedJavaCollectionStubMethod &&
            index == other.index &&
            parent == other.parent

    override fun hashCode(): Int = index * 31 + parent.hashCode()
}
