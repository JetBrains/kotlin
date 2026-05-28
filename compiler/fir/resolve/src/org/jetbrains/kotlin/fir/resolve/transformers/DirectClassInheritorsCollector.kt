/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.addDirectInheritors
import org.jetbrains.kotlin.fir.declarations.directInheritors
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassIdFromDependencies
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.ClassId

class DirectClassInheritorsCollector(
    override val session: FirSession
) : FirDefaultVisitorVoid(), SessionHolder {

    private fun collectInheritorsOfCorrespondingExpectClass(expectClassId: ClassId, expansionClass: FirRegularClass) {
        if (LanguageFeature.MultiPlatformProjects.isDisabled()) return
        val correspondingExpectClass = session.getRegularClassSymbolByClassIdFromDependencies(expectClassId)?.fir ?: return
        if (correspondingExpectClass.isExpect) {
            expansionClass.addDirectInheritors(correspondingExpectClass.directInheritors)
        }
    }

    private fun extractClassFromTypeRef(typeRef: FirTypeRef): FirRegularClass? {
        val lookupTag = typeRef.coneType.lookupTagIfAny ?: return null
        val classLikeSymbol: FirClassifierSymbol<*> = lookupTag.toSymbol(session) ?: return null
        return when (classLikeSymbol) {
            is FirRegularClassSymbol -> classLikeSymbol.fir
            is FirTypeAliasSymbol -> {
                classLikeSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
                extractClassFromTypeRef(classLikeSymbol.fir.expandedTypeRef)
            }
            else -> null
        }
    }

    override fun visitElement(element: FirElement): Unit = element.acceptChildren(this)

    override fun visitFile(file: FirFile): Unit = file.declarations.forEach { it.accept(this) }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        regularClass.declarations.forEach { it.accept(this) }

        val symbol = regularClass.symbol
        for (typeRef in regularClass.superTypeRefs) {
            val parent = extractClassFromTypeRef(typeRef) ?: continue
            parent.addDirectInheritors(symbol)
        }

        collectInheritorsOfCorrespondingExpectClass(symbol.classId, regularClass)
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        if (!typeAlias.isActual) return
        val expansionClass = typeAlias.expandedTypeRef.coneType.toRegularClassSymbol(session)?.fir ?: return
        collectInheritorsOfCorrespondingExpectClass(typeAlias.classId, expansionClass)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
        anonymousObject.declarations.forEach { it.accept(this) }

        val symbol = anonymousObject.symbol
        for (typeRef in anonymousObject.superTypeRefs) {
            val parent = extractClassFromTypeRef(typeRef) ?: continue
            parent.addDirectInheritors(symbol)
        }
    }

    override fun visitImport(import: FirImport): Unit = Unit

    override fun visitTypeRef(typeRef: FirTypeRef): Unit = Unit

    override fun visitReference(reference: FirReference): Unit = Unit

    override fun visitTypeProjection(typeProjection: FirTypeProjection): Unit = Unit

    override fun visitTypeParameter(typeParameter: FirTypeParameter): Unit = Unit

    override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus): Unit = Unit

    override fun visitAnnotation(annotation: FirAnnotation): Unit = Unit

    override fun visitLabel(label: FirLabel): Unit = Unit
}
