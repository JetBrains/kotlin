/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildDelegatedConstructorCall
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
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

            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin

            owner.typeParameterSymbols.mapTo(typeParameters) { buildConstructedClassTypeParameterRef { symbol = it } }
            returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
            status = generateStatus()
            if (owner.isInner) {
                val parentSymbol = owner.getContainingClassLookupTag()?.toSymbol(session) as? FirClassSymbol<*>
                    ?: error("Symbol for parent of $owner not found")
                dispatchReceiverType = parentSymbol.defaultType()
            }
            this@ConstructorBuildingContext.valueParameters.mapTo(valueParameters) { generateValueParameter(it, symbol, typeParameters) }
            if (owner is FirRegularClassSymbol) {
                owner.resolvedContextReceivers.mapTo(contextReceivers) {
                    buildContextReceiver { typeRef = it.typeRef.coneType.toFirResolvedTypeRef() }
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
            it.generateNoArgDelegatingConstructorCall(session)
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

private fun FirConstructor.generateNoArgDelegatingConstructorCall(session: FirSession) {
    val owner = returnTypeRef.coneType.toSymbol(session) as? FirClassSymbol<*>
    requireNotNull(owner)
    val delegatingConstructorCall = buildDelegatedConstructorCall {
        val superClasses = owner.resolvedSuperTypes.filter { it.toRegularClassSymbol(session)?.classKind == ClassKind.CLASS }
        val singleSupertype = when (superClasses.size) {
            0 -> session.builtinTypes.anyType.type
            1 -> superClasses.first()
            else -> error("Object $owner has more than one class supertypes: $superClasses")
        }
        constructedTypeRef = singleSupertype.toFirResolvedTypeRef()
        val superSymbol = singleSupertype.toRegularClassSymbol(session) ?: error("Symbol for supertype $singleSupertype not found")
        val superConstructorSymbol = superSymbol.declaredMemberScope(session, memberRequiredPhase = null)
            .getDeclaredConstructors()
            .firstOrNull { it.valueParameterSymbols.isEmpty() }
            ?: error("No arguments constructor for class $singleSupertype not found")
        calleeReference = buildResolvedNamedReference {
            name = superConstructorSymbol.name
            resolvedSymbol = superConstructorSymbol
        }
        argumentList = FirEmptyArgumentList
        isThis = false
    }
    replaceDelegatedConstructor(delegatingConstructorCall)
}
