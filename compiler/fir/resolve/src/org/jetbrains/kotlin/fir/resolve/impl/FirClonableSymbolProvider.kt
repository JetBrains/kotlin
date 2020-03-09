/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildClassImpl
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirClonableSymbolProvider(session: FirSession, scopeProvider: FirScopeProvider) : FirSymbolProvider() {
    companion object {
        val CLONABLE: Name = Name.identifier("Cloneable")
        val CLONABLE_CLASS_ID: ClassId = ClassId(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, CLONABLE)

        val CLONE: Name = Name.identifier("clone")
    }

    private val klass = buildClassImpl {
        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
        this.session = session
        status = FirDeclarationStatusImpl(
            Visibilities.PUBLIC,
            Modality.ABSTRACT
        )
        classKind = ClassKind.INTERFACE
        declarations += buildSimpleFunction {
            this.session = session
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            returnTypeRef = buildResolvedTypeRef {
                type = session.builtinTypes.anyType.type
            }
            status = FirDeclarationStatusImpl(Visibilities.PROTECTED, Modality.OPEN)
            name = CLONE
            symbol = FirNamedFunctionSymbol(CallableId(CLONABLE_CLASS_ID, CLONE))
        }
        this.scopeProvider = scopeProvider
        name = CLONABLE
        symbol = FirRegularClassSymbol(CLONABLE_CLASS_ID)
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return if (classId == CLONABLE_CLASS_ID) klass.symbol else null
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return emptyList()
    }

    override fun getNestedClassifierScope(classId: ClassId): FirScope? {
        return null
    }

    override fun getPackage(fqName: FqName): FqName? {
        return null
    }
}