/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.statusTransformerExtensions
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirGlobalResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirGlobalExtensionStatusProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession) {
    override fun process(files: Collection<FirFile>) {
        val extensions = session.extensionService.statusTransformerExtensions
        if (extensions.isEmpty()) return
        val provider = session.predicateBasedProvider
        for (extension in extensions) {
            val declarations = provider.getSymbolsWithOwnersByPredicate(extension.predicate)
            for ((declaration, owners) in declarations) {
                // TODO: maybe replace with visitor?
                if (declaration is FirStatusOwner) {
                    val newStatus = extension.transformStatus(declaration, owners, declaration.status)
                    declaration.transformStatus(ReplaceStatus, newStatus)
                }
            }
        }
    }
}

class FirTransformerBasedExtensionStatusProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer: FirTransformer<Nothing?> = StatusUpdater()

    private inner class StatusUpdater : FirDefaultTransformer<Nothing?>() {
        private val extensions = session.extensionService.statusTransformerExtensions
        private val predicateBasedProvider = session.predicateBasedProvider

        private fun FirStatusOwner.updateStatus() {
            if (extensions.isEmpty()) return
            val owners = predicateBasedProvider.getOwnersOfDeclaration(this as FirAnnotatedDeclaration<*>)
            requireNotNull(owners)
            var status = this.status
            for (extension in extensions) {
                status = extension.transformStatus(this, owners, status)
            }
            transformStatus(ReplaceStatus, status)
        }

        override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
            return element
        }

        override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): FirTypeAlias {
            typeAlias.updateStatus()
            return typeAlias
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
            regularClass.updateStatus()
            regularClass.transformDeclarations(this, data)
            return regularClass
        }

        override fun transformConstructor(constructor: FirConstructor, data: Nothing?): FirConstructor {
            constructor.updateStatus()
            return constructor
        }

        override fun transformProperty(property: FirProperty, data: Nothing?): FirProperty {
            property.updateStatus()
            property.transformGetter(this, data)
            property.transformSetter(this, data)
            return property
        }

        override fun transformField(field: FirField, data: Nothing?): FirField {
            field.updateStatus()
            return field
        }

        override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Nothing?): FirEnumEntry {
            enumEntry.updateStatus()
            return enumEntry
        }

        override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): FirSimpleFunction {
            simpleFunction.updateStatus()
            return simpleFunction
        }
    }
}

private object ReplaceStatus : FirTransformer<FirDeclarationStatus>() {
    override fun <E : FirElement> transformElement(element: E, data: FirDeclarationStatus): E {
        return element
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: FirDeclarationStatus
    ): FirDeclarationStatus {
        return data
    }
}
