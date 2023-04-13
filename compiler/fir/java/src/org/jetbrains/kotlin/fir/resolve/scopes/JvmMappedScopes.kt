/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.scopes

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.jvm.JvmMappedScope
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.name.StandardClassIds

fun wrapScopeWithJvmMapped(
    klass: FirClass,
    declaredMemberScope: FirContainingNamesAwareScope,
    useSiteSession: FirSession,
): FirContainingNamesAwareScope {
    if (klass !is FirRegularClass) return declaredMemberScope
    val classId = klass.classId
    val kotlinUnsafeFqName = classId.asSingleFqName().toUnsafe()
    val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(kotlinUnsafeFqName)
        ?: return declaredMemberScope
    val symbolProvider = useSiteSession.symbolProvider
    val javaClass = symbolProvider.getClassLikeSymbolByClassId(javaClassId)?.fir as? FirJavaClass
        ?: return declaredMemberScope

    // We don't add additional built-in members to function types and kotlin.Any (see JvmBuiltInsCustomizer.getJavaAnalogue)
    if (klass.symbol.toLookupTag().isSomeFunctionType(useSiteSession) || classId == StandardClassIds.Any) return declaredMemberScope

    return JvmMappedScope(
        useSiteSession,
        klass,
        javaClass,
        declaredMemberScope
    )
}
