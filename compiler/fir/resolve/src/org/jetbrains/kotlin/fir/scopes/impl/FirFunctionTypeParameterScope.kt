/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationContainer
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirFunctionTypeParameterScope(
    val file: FirFile,
    val className: FqName,
    val functionName: Name,
    val session: FirSession
) : FirScope {

    private val firProvider = FirProvider.getInstance(session)

    val typeParameterContainer = when {
        className.isRoot -> file
        else -> firProvider.getFirClassifierByFqName(ClassId(file.packageFqName, className, false)) as? FirDeclarationContainer
    }?.declarations?.filterIsInstance<FirNamedFunction>()?.find { it.name == functionName }

    val typeParameters = typeParameterContainer?.typeParameters.orEmpty().groupBy { it.name }

    override fun processClassifiersByName(name: Name, processor: (ConeSymbol) -> Boolean): Boolean {
        val matchedTypeParameters = typeParameters[name] ?: return true

        return matchedTypeParameters.all { processor(it.symbol) }
    }
}