/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies.semantics

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

sealed interface NodeIndex<out D : FirDeclaration> {

    data class DeclarationIndex<D : FirDeclaration>(
        val containingEntity: EnclosingEntity<*>,
        val symbol: FirBasedSymbol<D>
    ) : NodeIndex<D> {
        override fun toString(): String {
            val name = when (symbol) {
                is FirPropertySymbol -> symbol.name.asString()
                is FirFunctionSymbol<*> -> "${symbol.name.asString()}()"
                is FirAnonymousInitializerSymbol -> "<init_block>"
                else -> ""
            }
            return "${if (containingEntity is EnclosingEntity.File) "" else "$containingEntity."}$name"
        }
    }

    data class BeginSubgraphIndex<D : FirDeclaration>(
        val enclosingEntity: EnclosingEntity<D>
    ) : NodeIndex<D> {
        override fun toString(): String = when (enclosingEntity) {
            is EnclosingEntity.File -> "<$enclosingEntity>"
            is EnclosingEntity.Class -> "$enclosingEntity.<clinit>"
            else -> enclosingEntity.toString()
        }
    }

    data class EndSubgraphIndex<D : FirDeclaration>(
        val beginIndex: BeginSubgraphIndex<D>
    ) : NodeIndex<D> {
        override fun toString(): String = "<${beginIndex.enclosingEntity} initialized>"
    }

    data class CompositeIndex(
        val indices: Set<NodeIndex<*>>
    ) : NodeIndex<Nothing> {
        override fun toString(): String = indices.joinToString(prefix = "{", postfix = "}")
    }

    companion object {
        fun <D : FirDeclaration> EnclosingEntity<D>.beginIndex(): BeginSubgraphIndex<D> = BeginSubgraphIndex(this)
        fun <D : FirDeclaration> EnclosingEntity<D>.endIndex(): EndSubgraphIndex<D> = EndSubgraphIndex(beginIndex())
    }
}