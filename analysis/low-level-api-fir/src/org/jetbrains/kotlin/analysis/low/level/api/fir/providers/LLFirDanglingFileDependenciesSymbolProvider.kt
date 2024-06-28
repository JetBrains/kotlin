/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

class LLFirDanglingFileDependenciesSymbolProvider(private val delegate: FirSymbolProvider) : FirSymbolProvider(delegate.session) {
    override val symbolNamesProvider: FirSymbolNamesProvider
        get() = delegate.symbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return delegate.getClassLikeSymbolByClassId(classId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelCallableSymbols(packageFqName, name).let(::filterSymbols)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelFunctionSymbols(packageFqName, name).let(::filterSymbols)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += delegate.getTopLevelPropertySymbols(packageFqName, name).let(::filterSymbols)
    }

    override fun getPackage(fqName: FqName): FqName? {
        return delegate.getPackage(fqName)
    }

    // In complex projects, there might be several library copies (with the same or different versions).
    // As there is no way to build a reliable dependency graph between libraries, a project library depends on all other libraries.
    // As a result, there might be several declarations in the classpath with the same name and signature.
    // Normally, K2 issues a 'resolution ambiguity' error on calls to such libraries. It is sort of acceptable for resolution, as
    // resolution errors are never shown in the library code. However, the backend, to which 'evaluate expression' needs to pass FIR
    // afterwards, is not designed for compiling ambiguous (and non-completed) calls.
    // The code below scans for declaration duplicates, and chooses a number of them from a single class input (a JAR or a directory).
    // The logic is not ideal, as, in theory, versions might differ non-trivially: each artifact may have unique declarations.
    // However, such cases should be relatively rare, and to provide the candidate list more precisely, one would need to also compare
    // signatures of each declaration.
    private fun <T : FirCallableSymbol<*>> filterSymbols(symbols: List<T>): List<T> {
        if (symbols.size < 2) {
            return symbols
        }

        val binarySymbols = LinkedHashMap<CallableId, MutableMap<VirtualFile, MutableList<T>>>()
        val otherSymbols = ArrayList<T>()

        for (symbol in symbols) {
            if (symbol.callableId.className == null) {
                val callableId = symbol.callableId

                val symbolFile = symbol.fir.psi?.containingFile
                val symbolVirtualFile = symbolFile?.virtualFile
                if (symbolFile is KtFile && symbolFile.isCompiled && symbolVirtualFile != null) {
                    val symbolRootVirtualFile = getSymbolRootFile(symbolVirtualFile, symbolFile.packageFqName)
                    if (symbolRootVirtualFile != null) {
                        binarySymbols
                            .getOrPut(callableId, ::LinkedHashMap)
                            .getOrPut(symbolRootVirtualFile, ::ArrayList)
                            .add(symbol)
                        continue
                    }
                }
            }

            otherSymbols.add(symbol)
        }

        if (binarySymbols.isNotEmpty()) {
            return buildList {
                addAll(otherSymbols)
                for (binarySymbolGroup in binarySymbols.values) {
                    // For consistency with class symbol fetching, callable symbols are returned in the same order as indices returned.
                    val firstBinarySymbolGroupValue = binarySymbolGroup.values.first()
                    if (firstBinarySymbolGroupValue.isNotEmpty()) {
                        addAll(firstBinarySymbolGroupValue)
                    }
                }
            }
        }

        return symbols
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