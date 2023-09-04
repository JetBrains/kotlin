/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

internal class LLLibraryScopeAwareCallConflictResolverFactory(
    private val delegateFactory: ConeCallConflictResolverFactory
) : ConeCallConflictResolverFactory() {
    override fun create(
        typeSpecificityComparator: TypeSpecificityComparator,
        components: InferenceComponents,
        transformerComponents: BodyResolveComponents,
    ): ConeCallConflictResolver {
        val delegate = delegateFactory.create(typeSpecificityComparator, components, transformerComponents)
        return LLLibraryScopeAwareConeCallConflictResolver(delegate, transformerComponents)
    }
}

private class LLLibraryScopeAwareConeCallConflictResolver(
    private val delegate: ConeCallConflictResolver,
    private val bodyResolveComponents: BodyResolveComponents
) : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(candidates: Set<Candidate>, discriminateAbstracts: Boolean): Set<Candidate> {
        val filteredCandidates = when {
            candidates.size > 1 -> filterCodeFragmentCandidates(candidates)
            else -> candidates
        }

        return delegate.chooseMaximallySpecificCandidates(filteredCandidates, discriminateAbstracts)
    }

    // In complex projects, there might be several library copies (with the same or different versions).
    // As there is no way to build a reliable dependency graph between libraries, a project library depends on all other libraries.
    // As a result, there might be several declarations in the classpath with the same name and signature.
    // Normally, K2 issues a 'resolution ambiguity' error on calls to such libraries. It is sort of acceptable for resolution, as
    // resolution errors are never shown in the library code. However, the backend, to which 'evaluate expression' needs to pass FIR
    // afterwards, is not designed for compiling ambiguous (and non-completed) calls.
    // The code below scans for declaration duplicates, and chooses a set of them from a single class input (a JAR or a directory).
    // The logic is not ideal, as, in theory, versions might differ non-trivially: each artifact may have unique declarations.
    // However, such cases should be relatively rare, and to provide the candidate list more precisely, one would need to also compare
    // signatures of each declaration.
    private fun filterCodeFragmentCandidates(candidates: Set<Candidate>): Set<Candidate> {
        val binaryCallableCandidates = LinkedHashMap<CallableId, MutableMap<VirtualFile, MutableList<Candidate>>>()
        val otherCandidates = ArrayList<Candidate>()

        for (candidate in candidates) {
            val symbol = candidate.symbol

            if (symbol is FirCallableSymbol<*> && symbol.callableId.className == null) {
                val callableId = symbol.callableId

                val symbolFile = symbol.fir.psi?.containingFile
                val symbolVirtualFile = symbolFile?.virtualFile
                if (symbolFile is KtFile && symbolFile.isCompiled && symbolVirtualFile != null) {
                    val symbolRootVirtualFile = getSymbolRootFile(symbolVirtualFile, symbolFile.packageFqName)
                    if (symbolRootVirtualFile != null) {
                        binaryCallableCandidates
                            .getOrPut(callableId, ::LinkedHashMap)
                            .getOrPut(symbolRootVirtualFile, ::ArrayList)
                            .add(candidate)
                        continue
                    }
                }
            }

            otherCandidates.add(candidate)
        }

        if (binaryCallableCandidates.isNotEmpty()) {
            return buildSet {
                addAll(otherCandidates)
                for (binaryCallableCandidateGroup in binaryCallableCandidates.values) {
                    val chosenGroupKey = binaryCallableCandidateGroup.keys.maxBy { it.path }
                    addAll(binaryCallableCandidateGroup[chosenGroupKey].orEmpty())
                }
            }
        }

        return candidates
    }

    private fun getSymbolRootFile(virtualFile: VirtualFile, packageFqName: FqName): VirtualFile? {
        val packageFqNameSegments = packageFqName.pathSegments().asReversed()
        val nestingLevel = packageFqNameSegments.size

        var current = virtualFile
        var index = 0

        while (true) {
            assert(index <= nestingLevel)

            val parent = current.parent ?: return null

            if (index == nestingLevel) {
                // Parent containing the root package is a class file root
                return parent
            }

            if (parent.name != packageFqNameSegments[index].asString()) {
                // Unexpected directory structure, the class is in a non-conventional root
                return null
            }

            current = parent
            index += 1
        }
    }
}