/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// This symbol provider only loads JVM classes *not* annotated with Kotlin `@Metadata` annotation.
// Use it in application sessions for loading classes from Java files listed on the command line.
// For library and incremental compilation sessions use `KotlinDeserializedJvmSymbolsProvider`
// in order to load Kotlin classes as well.
class JavaSymbolProvider(
    session: FirSession,
    private val javaFacade: FirJavaFacade,
) : FirSymbolProvider(session) {

    private val classCache =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = { classId: ClassId, parentClassSymbol: FirRegularClassSymbol? ->
                javaFacade.findClass(classId)?.let { FirRegularClassSymbol(classId) to (it to parentClassSymbol) }
                    ?: (null to (null to null))
            },
            postCompute = { _, classSymbol, (javaClass, parentClassSymbol) ->
                if (classSymbol != null && javaClass != null) {
                    javaFacade.convertJavaClassToFir(classSymbol, parentClassSymbol, javaClass)
                }
            }
        )

    override fun getPackage(fqName: FqName): FqName? =
        javaFacade.getPackage(fqName)

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
        try {
            if (javaFacade.hasTopLevelClassOf(classId)) getFirJavaClass(classId) else null
        } catch (e: ProcessCanceledException) {
            null
        }

    private fun getFirJavaClass(classId: ClassId): FirRegularClassSymbol? =
        classCache.getValue(classId, classId.outerClassId?.let { getFirJavaClass(it) })

    @OptIn(FirSymbolProviderInternals::class)
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {}

    @OptIn(FirSymbolProviderInternals::class)
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {}

    @OptIn(FirSymbolProviderInternals::class)
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {}
}
