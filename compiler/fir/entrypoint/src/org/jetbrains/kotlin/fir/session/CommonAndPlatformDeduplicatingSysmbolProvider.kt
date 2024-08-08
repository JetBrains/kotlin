/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(SymbolInternals::class)
class CommonAndPlatformDeduplicatingSysmbolProvider(
    session: FirSession,
    val commonSymbolProvider: FirSymbolProvider,
    val platformSymbolProvider: FirSymbolProvider,
) : FirSymbolProvider(session) {
    val providers: List<FirSymbolProvider> = listOf(commonSymbolProvider, platformSymbolProvider)

    override val symbolNamesProvider: FirSymbolNamesProvider = FirCompositeSymbolNamesProvider.fromSymbolProviders(providers)

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return providers.flatMap { it.getTopLevelCallableSymbols(packageFqName, name) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += getTopLevelCallableSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        providers.forEach {
            it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        providers.forEach {
            it.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        return providers.firstNotNullOfOrNull { it.getPackage(fqName) }
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }
    }

    private fun filterOutEquivalentCalls(candidates: Collection<FirCallableSymbol<*>>): Set<FirCallableSymbol<*>> {
        // Since we can consider a declaration from source and one from binary equivalent, we need to make sure we favor the one from
        // source, otherwise we might get a behavior change to K1.
        // See org.jetbrains.kotlin.resolve.calls.results.OverloadingConflictResolver.filterOutEquivalentCalls.
        val fromSourceFirst = candidates.sortedBy { it.fir.moduleData.session.kind != FirSession.Kind.Source }

        val result = mutableSetOf<FirCallableSymbol<*>>()
        outerLoop@ for (myCandidate in fromSourceFirst) {
            val me = myCandidate.fir
            if (true && me.symbol.containingClassLookupTag() == null) {
                val resultIterator = result.iterator()
                while (resultIterator.hasNext()) {
                    val otherCandidate = resultIterator.next()
                    val other = otherCandidate.fir
                    if (true && other.symbol.containingClassLookupTag() == null) {
                        if (areEquivalentTopLevelCallables(me, other)) {
                            /**
                             * If we have an expect function in the result set and encounter a non-expect function among non-processed
                             * candidates, then we need to prefer this new function to the original expect one
                             */
                            if (other.isExpect && !me.isExpect) {
                                resultIterator.remove()
                            } else {
                                continue@outerLoop
                            }
                        }
                    }
                }
            }
            result += myCandidate
        }
        return result
    }

    private fun areEquivalentTopLevelCallables(
        first: FirCallableDeclaration,
        second: FirCallableDeclaration,
    ): Boolean {
        if (first.symbol.callableId != second.symbol.callableId) return false
        // Emulate behavior from K1 where declarations from the same source module are never equivalent.
        // We expect REDECLARATION or CONFLICTING_OVERLOADS to be reported in those cases.
        // See a.containingDeclaration == b.containingDeclaration check in
        // org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent.
        // We can't rely on the fact that library declarations will have different moduleData, e.g. in Native metadata compilation,
        // multiple stdlib declarations with the same moduleData can be present, see KT-61461.
        if (first.moduleData == second.moduleData && first.moduleData.session.kind == FirSession.Kind.Source) return false
        if (first is FirVariable != second is FirVariable) {
            return false
        }
        if (!first.symbol.mappedArgumentsOrderRepresentation.contentEquals(second.symbol.mappedArgumentsOrderRepresentation)) {
            return false
        }

        val overrideChecker = FirStandardOverrideChecker(session)
        return if (first is FirProperty && second is FirProperty) {
            overrideChecker.isOverriddenProperty(first, second, ignoreVisibility = true) &&
                    overrideChecker.isOverriddenProperty(second, first, ignoreVisibility = true)
        } else if (first is FirSimpleFunction && second is FirSimpleFunction) {
            overrideChecker.isOverriddenFunction(first, second, ignoreVisibility = true) &&
                    overrideChecker.isOverriddenFunction(second, first, ignoreVisibility = true)
        } else {
            false
        }
    }

    private val FirCallableSymbol<*>.mappedArgumentsOrderRepresentation: IntArray?
        get() {
            val function = fir as? FirFunction ?: return null
            val parametersToIndices = function.valueParameters.mapIndexed { index, it -> it to index }.toMap()
            val mapping = function.valueParameters
            val result = IntArray(mapping.size + 1) { function.valueParameters.size }
            for ((index, parameter) in mapping.withIndex()) {
                result[index + 1] = parametersToIndices[parameter] ?: error("Unmapped argument in arguments mapping")
            }
            return result
        }
}