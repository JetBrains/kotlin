/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.analysis.checkers.hasAnnotationOrInsideAnnotatedClass
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinsSymbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

/**
 * It's initialized on the top of module-based providers for platform source sets.
 * It shouldn't be used if platform source-set is single (for jdk7, jdk8 modules)
 *
 * If compiler encounters an actual declaration with a corresponding expect one
 * marked with `ActualizeByJvmBuiltinProvider` annotation, then
 * `FirJvmActualizingBuiltinSymbolProvider` tries to return a member from `FirBuiltinSymbolProvider`
 * instead of falling through to an expect declaration in common source set.
 *
 * It's needed while initializing `Fir2IrLazyClass` to prevent resolving
 * to symbols from common source set
 *
 * Those actuals can be treated as declarations in a virtual file
 *
 * Fill the `expectForActual` map on successful matches here because `FirExpectActualMatcherTransformer` handles actuals only from sources
 */
class FirJvmActualizingBuiltinSymbolProvider(
    val builtinsSymbolProvider: FirBuiltinsSymbolProvider,
    private val refinedSourceSymbolProviders: List<FirSymbolProvider>,
) : FirSymbolProvider(builtinsSymbolProvider.session) {
    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        for (symbolProvider in refinedSourceSymbolProviders) {
            val classSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: continue
            if (!classSymbol.hasAnnotationOrInsideAnnotatedClass(
                    StandardClassIds.Annotations.ActualizeByJvmBuiltinProvider,
                    symbolProvider.session
                )
            ) {
                continue
            }

            require(classSymbol.isExpect)

            // Assume an `actual` always exists for the found `expect`
            // Otherwise it's something wrong with stdlib sources
            return builtinsSymbolProvider.getClassLikeSymbolByClassId(classId)!!.also {
                it.fir.expectForActual = mapOf<ExpectActualMatchingCompatibility, List<FirBasedSymbol<*>>>(
                    ExpectActualMatchingCompatibility.MatchedSuccessfully to listOf(classSymbol)
                )
            }
        }

        return null
    }

    override val symbolNamesProvider: FirSymbolNamesProvider = builtinsSymbolProvider.symbolNamesProvider

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        getTopLevelSymbolsTo(destination) { provider, localDestination ->
            provider.getTopLevelCallableSymbolsTo(localDestination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelSymbolsTo(destination) { provider, localDestination ->
            provider.getTopLevelFunctionSymbolsTo(localDestination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        getTopLevelSymbolsTo(destination) { provider, localDestination ->
            provider.getTopLevelPropertySymbolsTo(localDestination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    private fun <T : FirCallableSymbol<*>> getTopLevelSymbolsTo(
        destination: MutableList<T>,
        getFunction: (FirSymbolProvider, MutableList<T>) -> Unit,
    ) {
        val tempDestination = mutableListOf<T>()
        for (symbolProvider in refinedSourceSymbolProviders) {
            tempDestination.clear()
            getFunction(symbolProvider, tempDestination)

            // Make sure there is a single `expect` with `@ActualizeByJvmBuiltinProvider` annotation
            // To match it possible to match it with a corresponding `actual`
            val expectSymbol = tempDestination.singleOrNull {
                it.hasAnnotation(StandardClassIds.Annotations.ActualizeByJvmBuiltinProvider, symbolProvider.session)
            } ?: continue
            require(expectSymbol.isExpect)

            val expectDeclarationReceiverType = (expectSymbol.resolvedReceiverTypeRef?.coneType as? ConeClassLikeType)?.classId

            tempDestination.clear()
            getFunction(builtinsSymbolProvider, tempDestination)

            // Typically, there is only one `actual` that matches the found `expect`
            // The single case with multiple actuals is `toString` with different receivers (String?, BigDecimal, BigInteger)
            // Fortunately, they can be distinguished just by the receiver type
            val actualSymbol = tempDestination.single {
                (it.resolvedReceiverTypeRef?.coneType as? ConeClassLikeType)?.classId == expectDeclarationReceiverType
            }

            actualSymbol.fir.expectForActual = mapOf<ExpectActualMatchingCompatibility, List<FirBasedSymbol<*>>>(
                ExpectActualMatchingCompatibility.MatchedSuccessfully to listOf(expectSymbol)
            )

            destination.add(actualSymbol)

            return
        }
    }

    override fun getPackage(fqName: FqName): FqName? = builtinsSymbolProvider.getPackage(fqName)
}