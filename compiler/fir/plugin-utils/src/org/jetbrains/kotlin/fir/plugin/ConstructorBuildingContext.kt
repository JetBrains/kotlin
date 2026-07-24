/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildDelegatedConstructorCall
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.callableIdForConstructor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

public class ConstructorBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>,
    private val isPrimary: Boolean
) : FunctionBuildingContext<FirConstructor>(owner.classId.callableIdForConstructor(), session, key, owner) {
    override fun build(): FirConstructor {
        requireNotNull(owner)
        val init: FirAbstractConstructorBuilder.() -> Unit = {
            symbol = FirConstructorSymbol(owner.classId)
            source = getSourceForFirDeclaration()

            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin

            owner.typeParameterSymbols.mapTo(typeParameters) { buildConstructedClassTypeParameterRef { symbol = it } }
            returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
            status = generateStatus()
            isLocal = owner.isLocal
            if (owner.isInner) {
                val parentSymbol = owner.getContainingClassLookupTag()?.toClassSymbol(session)
                    ?: error("Symbol for parent of $owner not found")
                dispatchReceiverType = parentSymbol.defaultType()
            }
            this@ConstructorBuildingContext.valueParameters.mapTo(valueParameters) { generateValueParameter(it, symbol, typeParameters) }
            if (owner is FirRegularClassSymbol) {
                owner.resolvedContextParameters.mapTo(contextParameters) {
                    buildValueParameterCopy(it) {
                        symbol = FirValueParameterSymbol()
                        containingDeclarationSymbol = owner
                    }
                }
            }
        }
        val constructor = if (isPrimary) {
            buildPrimaryConstructor(init)
        } else {
            buildConstructor(init)
        }
        constructor.containingClassForStaticMemberAttr = owner.toLookupTag()
        return constructor
    }

    /**
     * Type parameters of constructor are inherited from constructed class
     */
    @Deprecated("This function does nothing and should not be called", level = DeprecationLevel.HIDDEN)
    override fun typeParameter(
        name: Name,
        variance: Variance,
        isReified: Boolean,
        key: GeneratedDeclarationKey,
        config: TypeParameterBuildingContext.() -> Unit
    ) {
        shouldNotBeCalled()
    }

    /**
     * Context receivers of constructor are inherited from constructed class
     */
    @Deprecated("This function does nothing and should not be called", level = DeprecationLevel.HIDDEN)
    override fun contextReceiver(type: ConeKotlinType) {
        shouldNotBeCalled()
    }

    /**
     * Context receivers of constructor are inherited from constructed class
     */
    @Deprecated("This function does nothing and should not be called", level = DeprecationLevel.HIDDEN)
    override fun contextReceiver(typeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType) {
        shouldNotBeCalled()
    }
}

// ---------------------------------------------------------------------------------------------------------------------

/**
 * Creates constructor for [owner] class.
 * Created constructor is public, unless [config] changes this.
 *
 * [generateDelegatedNoArgConstructorCall] specifies whether default delegating constructor call to superclass should be generated.
 * This generation works only if superclass of the [owner] has constructor without arguments.
 * Custom delegated constructor calls should be generated in IR backend (see `IrGenerationExtension`).
 */
public fun FirExtension.createConstructor(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    isPrimary: Boolean = false,
    generateDelegatedNoArgConstructorCall: Boolean = false,
    config: ConstructorBuildingContext.() -> Unit = {}
): FirConstructor {
    return ConstructorBuildingContext(session, key, owner, isPrimary).apply(config).apply {
        status {
            isExpect = owner.isExpect
        }
    }.build().also {
        if (generateDelegatedNoArgConstructorCall) {
            it.tryPopulatingNoArgDelegatingConstructorCall(session)
        }
    }
}

/**
 * Creates private primary constructor without parameters for [owner] object.
 *
 * This is a shorthand for [createConstructor] which is useful for creating constructors for companions and other objects, as they should be private.
 *
 * [generateDelegatedNoArgConstructorCall] specifies whether default delegating constructor call to superclass should be generated.
 * This generation works only if superclass of the [owner] has constructor without arguments.
 * Custom delegated constructor calls should be generated in IR backend (see `IrGenerationExtension`).
 */
public fun FirExtension.createDefaultPrivateConstructor(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    generateDelegatedNoArgConstructorCall: Boolean = true
): FirConstructor {
    return createConstructor(owner, key, isPrimary = true, generateDelegatedNoArgConstructorCall) {
        visibility = Visibilities.Private
    }
}

private fun FirConstructor.tryPopulatingNoArgDelegatingConstructorCall(session: FirSession) {
    val owner = returnTypeRef.coneType.toClassSymbol(session)
    requireNotNull(owner)
    replaceDelegatedConstructor(owner.tryGeneratingNoArgDelegatingConstructorCall(session))
}

/**
 * Attempts to generate a no-argument delegating constructor call for the given class symbol.
 *
 * It returns `null` if the correct delegated constructor call can't be generated for some reason.
 */
public fun FirClassSymbol<*>.tryGeneratingNoArgDelegatingConstructorCall(session: FirSession): FirDelegatedConstructorCall? {
    return buildDelegatedConstructorCall {
        val superClassSymbol = getSuperClassSymbolOrAny(session) ?: return null
        constructedTypeRef = superClassSymbol.defaultType().toFirResolvedTypeRef()
        val superConstructorSymbol = superClassSymbol.declaredMemberScope(session, memberRequiredPhase = null)
            .getDeclaredConstructors()
            .firstOrNull { it.valueParameterSymbols.isEmpty() } ?: return null
        calleeReference = buildResolvedNamedReference {
            name = superConstructorSymbol.name
            resolvedSymbol = superConstructorSymbol
        }
        argumentList = FirEmptyArgumentList
        isThis = false
    }
}
