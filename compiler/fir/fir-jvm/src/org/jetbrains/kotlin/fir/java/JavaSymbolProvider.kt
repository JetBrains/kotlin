/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProviderWithoutCallables
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

// This symbol provider only loads JVM classes *not* annotated with Kotlin `@Metadata` annotation.
// Use it in application sessions for loading classes from Java files listed on the command line.
// For library and incremental compilation sessions use `KotlinDeserializedJvmSymbolsProvider`
// in order to load Kotlin classes as well.
// Also used in IDE for loading java classes separately from stub based kotlin classes
// This symbol provider should not have access to any extension provider (`FirDeclarationGenerationExtension`);
// otherwise it could provoke infinity recursion because an extension provider may check if a Java class is already existed
open class JavaSymbolProvider(
    session: FirSession,
    protected val javaFacade: FirJavaFacade,
) : FirSymbolProvider(session) {
    protected class ClassCacheContext(
        val parentClassSymbol: FirRegularClassSymbol? = null,
        val foundJavaClass: JavaClass? = null,
    )

    protected val classCache: FirCache<ClassId, FirRegularClassSymbol?, ClassCacheContext?> =
        session.firCachesFactory.createCache createValue@{ classId, context ->
            val javaClass = context?.foundJavaClass ?: javaFacade.findClass(classId) ?: return@createValue null
            val symbol = FirRegularClassSymbol(classId)
            javaFacade.convertJavaClassToFir(symbol, context?.parentClassSymbol, javaClass)
            symbol
        }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
        if (javaFacade.hasTopLevelClassOf(classId)) getClassLikeSymbolByClassId(classId, null) else null

    fun getClassLikeSymbolByClassId(classId: ClassId, javaClass: JavaClass?): FirRegularClassSymbol? =
        classCache.getValue(
            classId,
            ClassCacheContext(
                parentClassSymbol = classId.outerClassId?.let { getClassLikeSymbolByClassId(it, null) },
                foundJavaClass = javaClass,
            )
        )

    @OptIn(FirSymbolProviderInternals::class)
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {}

    @OptIn(FirSymbolProviderInternals::class)
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {}

    @OptIn(FirSymbolProviderInternals::class)
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {}

    override fun hasPackage(fqName: FqName): Boolean = javaFacade.hasPackage(fqName)

    override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProviderWithoutCallables() {
        override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            javaFacade.knownClassNamesInPackage(packageFqName)?.mapToSetOrEmpty { Name.identifier(it) }
    }
}

val FirSession.javaSymbolProvider: JavaSymbolProvider? by FirSession.nullableSessionComponentAccessor()

/**
 * Look up a class-like Java symbol (source or binary) by [classId], independent of the composite
 * [org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider] resolution order.
 *
 * Used by indirect callers that need to detect *Java* declarations even when a Kotlin declaration
 * with the same [classId] exists (e.g. JVM redeclaration diagnostics, Kotlin-to-Java direct
 * actualization, Lombok builder discovery). `session.symbolProvider` is unsuitable for this
 * because [org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider.getClassLikeSymbolByClassId]
 * returns the first non-null match — for shared class ids the Kotlin source provider wins and the
 * Java symbol is hidden.
 *
 * Today this delegates to [javaSymbolProvider] (whose backing `CombinedJavaClassFinder` covers
 * both source and binary Java classes). After Stage 2 §6.2 / §6.3 (see
 * `compiler/java-direct/implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md`), `JavaSymbolProvider`
 * narrows to source-only and binary Java lookups move into `JvmClassFileBasedSymbolProvider`;
 * this helper will then also consult the deserializer for binary `FirDeclarationOrigin.Java.Library`
 * results, transparently to its callers.
 */
fun FirSession.getJavaClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
    javaSymbolProvider?.getClassLikeSymbolByClassId(classId)
