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

    // Stage 2 §6.2 + §6.5 (`compiler/java-direct/implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md`):
    // narrow `JavaSymbolProvider` to Java *source* classes only. The class-id gate moves from
    // `javaFacade.hasTopLevelClassOf(classId)` (source∪binary) to `javaFacade.isInSourceIndex(classId)`
    // — on `java-direct` this delegates to `JavaClassFinderOverAstImpl.isClassInIndex`; on
    // non-`java-direct` single-side finders the default returns `true` and narrowing is a no-op.
    // Binary Java classes flow through `JvmClassFileBasedSymbolProvider` via the deserializer-owned
    // `JvmBinaryClassFinderInputs` adapter (`JvmBinaryClassFinderInputsOverIndex` on the java-direct
    // path, fallback to `FirJavaFacade` elsewhere). The source∪binary names union is reconstituted
    // at the composite-symbol-names-provider layer, where this source-only set is joined with
    // `JvmClassFileBasedSymbolProvider.knownTopLevelClassesInPackage`.
    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
        if (javaFacade.isInSourceIndex(classId)) getClassLikeSymbolByClassId(classId, null) else null

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

    override fun hasPackage(fqName: FqName): Boolean = javaFacade.hasPackageInSources(fqName)

    override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProviderWithoutCallables() {
        override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            javaFacade.sourceClassNamesInPackage(packageFqName)?.mapToSetOrEmpty { Name.identifier(it) }
    }
}

val FirSession.javaSymbolProvider: JavaSymbolProvider? by FirSession.nullableSessionComponentAccessor()

/**
 * Look up a class-like Java symbol by [classId], independent of the composite
 * [org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider] resolution order.
 *
 * Used by indirect callers that need to detect *Java* declarations even when a Kotlin declaration
 * with the same [classId] exists (e.g. JVM redeclaration diagnostics, Kotlin-to-Java direct
 * actualization, Lombok builder discovery). `session.symbolProvider` is unsuitable for this
 * because [org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider.getClassLikeSymbolByClassId]
 * returns the first non-null match — for shared class ids the Kotlin source provider wins and the
 * Java symbol is hidden.
 *
 * Delegates to [javaSymbolProvider]. After Stage 2 §6.2 (see
 * `compiler/java-direct/implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md`), `JavaSymbolProvider` is
 * narrowed to Java *source* classes only; this helper therefore now sees source Java symbols only.
 *
 * Binary Java visibility (i.e. `FirDeclarationOrigin.Java.Library`) is deferred to Stage 2 §6.3,
 * which moves binary lookups into `JvmClassFileBasedSymbolProvider` and at the same point will
 * extend this helper to additionally consult the deserializer with proper local-class /
 * cross-session scoping (a naive composite walk does not work — see
 * `compiler/java-direct/ITERATION_RESULTS.md` "Stage 2 §6.2" key learnings).
 *
 * Each of the three callers is OK with the current source-only behavior:
 *   - `FirDirectJavaActualDeclarationExtractor` already filters `FirDeclarationOrigin.Java.Source`;
 *   - `Lombok AbstractBuilderGenerator` discovers Lombok-annotated *source* classes;
 *   - `FirJvmConflictsChecker` historically also reported Kotlin vs binary-Java redeclarations,
 *     but the java-direct test suite has no such fixture; §6.3 will restore that coverage.
 */
fun FirSession.getJavaClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
    javaSymbolProvider?.getClassLikeSymbolByClassId(classId)
