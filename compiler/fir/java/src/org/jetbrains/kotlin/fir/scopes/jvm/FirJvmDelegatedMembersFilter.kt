/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.scopes.impl.FirDelegatedMembersFilter
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds

/*
 * KT-18324: If original is java method with default then delegated function should not be generated
 * See org.jetbrains.kotlin.resolve.jvm.JvmDelegationFilter for K1 implementation
 */
class FirJvmDelegatedMembersFilter(private val session: FirSession) : FirDelegatedMembersFilter() {
    companion object {
        private val PLATFORM_DEPENDENT_ANNOTATION_CLASS_ID = ClassId.topLevel(FqName("kotlin.internal.PlatformDependent"))
    }

    override fun shouldNotGenerateDelegatedMember(memberSymbolFromSuperInterface: FirCallableSymbol<*>): Boolean {
        val original = memberSymbolFromSuperInterface.unwrapFakeOverrides()
        return original.isNonAbstractJavaMethod() || original.hasJvmDefaultAnnotation() || original.isBuiltInMemberMappedToJavaDefault() || original.origin == FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk
    }

    // If java interface method is not abstract, then it's a default method.
    private fun FirCallableSymbol<*>.isNonAbstractJavaMethod(): Boolean {
        return origin == FirDeclarationOrigin.Enhancement && fir.modality != Modality.ABSTRACT
    }

    private fun FirCallableSymbol<*>.hasJvmDefaultAnnotation(): Boolean {
        return annotations.hasAnnotation(JvmStandardClassIds.JVM_DEFAULT_CLASS_ID, session)
    }

    private fun FirCallableSymbol<*>.isBuiltInMemberMappedToJavaDefault(): Boolean {
        return fir.modality != Modality.ABSTRACT &&
                annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_CLASS_ID, session)
    }
}
