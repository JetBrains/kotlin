/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.java.scopes.JavaAnnotationSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.java.scopes.JavaClassMembersEnhancementScope
import org.jetbrains.kotlin.fir.java.scopes.JavaClassStaticEnhancementScope
import org.jetbrains.kotlin.fir.java.scopes.JavaClassStaticUseSiteScope
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticNamesProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirUnstableSmartcastTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScope
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.load.java.possibleGetMethodNames
import org.jetbrains.kotlin.load.java.setMethodName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

@NoMutableState
class FirJavaSyntheticNamesProvider : FirSyntheticNamesProvider() {
    override fun possibleGetterNamesByPropertyName(name: Name): List<Name> = possibleGetMethodNames(name)
    override fun setterNameByGetterName(name: Name): Name = setMethodName(getMethodName = name)
    override fun possiblePropertyNamesByAccessorName(name: Name): List<Name> = getPropertyNamesCandidatesByAccessorName(name)

    val cache = mutableMapOf<ClassId, Boolean>()

    private fun FirTypeRef.toDeclaration(session: FirSession) = toClassLikeSymbol(session)?.fir as? FirClass

    private fun FirClass.mayContainSynthetics(session: FirSession): Boolean {
        cache[classId]?.let { return it }
        val superTypes = superTypeRefs.mapNotNull { it.toDeclaration(session) }
        val extendsSyntheticsOwner = superTypes.any { it.mayContainSynthetics(session) }
        val result = isJava || extendsSyntheticsOwner
        return result.also { cache[classId] = result }
    }

    override fun scopeMayContainSynthetics(scope: FirScope, session: FirSession): Boolean {
        return when (scope) {
            is AbstractFirUseSiteMemberScope -> scope.klass.mayContainSynthetics(session)
            is JavaClassMembersEnhancementScope -> scope.owner.mayContainSynthetics(session)
            is JavaClassStaticUseSiteScope -> scope.klass.mayContainSynthetics(session)
            is JavaClassStaticEnhancementScope -> scope.owner.mayContainSynthetics(session)
            is JavaAnnotationSyntheticPropertiesScope -> scope.owner.mayContainSynthetics(session)
            is FirClassSubstitutionScope -> scopeMayContainSynthetics(scope.useSiteMemberScope, session)
            is FirUnstableSmartcastTypeScope -> scope.scopes.any { scopeMayContainSynthetics(it, session) }
            is FirTypeIntersectionScope -> scope.scopes.any { scopeMayContainSynthetics(it, session) }
            else -> false
        }
    }
}
