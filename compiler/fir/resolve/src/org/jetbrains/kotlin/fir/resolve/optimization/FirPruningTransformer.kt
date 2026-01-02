package org.jetbrains.kotlin.fir.resolve.optimization

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.expressions.FirStatement

class FirPruningTransformer(
    private val reachable: Set<FirBasedSymbol<*>>
) : FirTransformer<Nothing?>() {

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        val newDeclarations = file.declarations.filter { shouldKeep(it) }
        file.replaceDeclarations(newDeclarations)

        file.transformDeclarations(this, data)
        return file
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        val newDeclarations = regularClass.declarations.filter { shouldKeep(it) }
        regularClass.replaceDeclarations(newDeclarations)

        regularClass.transformDeclarations(this, data)
        return regularClass
    }
    
    private fun shouldKeep(declaration: FirDeclaration): Boolean {
        return reachable.contains(declaration.symbol)
    }
}

