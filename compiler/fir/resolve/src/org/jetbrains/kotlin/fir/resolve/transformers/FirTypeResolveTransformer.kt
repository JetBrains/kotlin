/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirTypeResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirTypeResolveTransformer(session, scopeSession)
}

fun <F : FirClass<F>> F.runTypeResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<FirScope>,
): F {
    val transformer = FirTypeResolveTransformer(session, scopeSession, currentScopeList)

    return this.transform<F, Nothing?>(transformer, null).single
}

class FirTypeResolveTransformer(
    override val session: FirSession,
    scopeSession: ScopeSession,
    initialScopes: List<FirScope> = emptyList()
) : FirAbstractTreeTransformerWithSuperTypes(
    phase = FirResolvePhase.TYPES,
    scopeSession
) {

    init {
        scopes.addAll(initialScopes.asReversed())
    }

    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(session)
    private var currentFile: FirFile? = null

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        checkSessionConsistency(file)
        currentFile = file
        return withScopeCleanup {
            scopes.addAll(createImportingScopes(file, session, scopeSession))
            super.transformFile(file, data)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirStatement> {
        withScopeCleanup {
            regularClass.addTypeParametersScope()
            regularClass.typeParameters.forEach {
                it.accept(this, data)
            }
            unboundCyclesInTypeParametersSupertypes(regularClass)
        }

        return resolveNestedClassesSupertypes(regularClass, data)
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Nothing?): CompositeTransformResult<FirStatement> {
        return resolveNestedClassesSupertypes(anonymousObject, data)
    }

    override fun transformConstructor(constructor: FirConstructor, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            constructor.addTypeParametersScope()
            transformDeclaration(constructor, data)
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            typeAlias.addTypeParametersScope()
            transformDeclaration(typeAlias, data)
        }
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        enumEntry.replaceResolvePhase(FirResolvePhase.TYPES)
        enumEntry.transformReturnTypeRef(this, data)
        enumEntry.transformTypeParameters(this, data)
        enumEntry.transformAnnotations(this, data)
        return enumEntry.compose()
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            property.addTypeParametersScope()
            property.replaceResolvePhase(FirResolvePhase.TYPES)
            property.transformTypeParameters(this, data)
                .transformReturnTypeRef(this, data)
                .transformReceiverTypeRef(this, data)
                .transformGetter(this, data)
                .transformSetter(this, data)
                .transformAnnotations(this, data)
            if (property.isFromVararg == true) {
                property.transformTypeToArrayType()
                property.getter?.transformReturnTypeRef(StoreType, property.returnTypeRef)
                property.setter?.valueParameters?.map { it.transformReturnTypeRef(StoreType, property.returnTypeRef) }
            }

            unboundCyclesInTypeParametersSupertypes(property)

            property.compose()
        }
    }

    override fun transformField(field: FirField, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            field.replaceResolvePhase(FirResolvePhase.TYPES)
            field.transformReturnTypeRef(this, data).transformAnnotations(this, data)
            field.compose()
        }
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            simpleFunction.addTypeParametersScope()
            transformDeclaration(simpleFunction, data).also {
                unboundCyclesInTypeParametersSupertypes(it.single as FirTypeParametersOwner)
            }
        }
    }

    private fun unboundCyclesInTypeParametersSupertypes(typeParametersOwner: FirTypeParameterRefsOwner) {
        for (typeParameter in typeParametersOwner.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            if (hasSupertypePathToParameter(typeParameter, typeParameter, mutableSetOf())) {
                // TODO: Report diagnostic somewhere
                typeParameter.replaceBounds(
                    listOf(session.builtinTypes.nullableAnyType)
                )
            }
        }
    }

    private fun hasSupertypePathToParameter(
        currentTypeParameter: FirTypeParameter,
        typeParameter: FirTypeParameter,
        visited: MutableSet<FirTypeParameter>
    ): Boolean {
        if (visited.isNotEmpty() && currentTypeParameter == typeParameter) return true
        if (!visited.add(currentTypeParameter)) return false

        return currentTypeParameter.bounds.any {
            val nextTypeParameter = it.coneTypeSafe<ConeTypeParameterType>()?.lookupTag?.typeParameterSymbol?.fir ?: return@any false

            hasSupertypePathToParameter(nextTypeParameter, typeParameter, visited)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        if (implicitTypeRef is FirImplicitBuiltinTypeRef) return transformTypeRef(implicitTypeRef, data)
        return implicitTypeRef.compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirResolvedTypeRef> {
        return typeResolverTransformer.withFile(currentFile) { typeRef.transform(typeResolverTransformer, towerScope) }
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): CompositeTransformResult<FirStatement> {
        valueParameter.transformReturnTypeRef(this, data)
        valueParameter.transformAnnotations(this, data)
        valueParameter.transformVarargTypeToArrayType()
        return valueParameter.compose()
    }

    override fun transformBlock(block: FirBlock, data: Nothing?): CompositeTransformResult<FirStatement> {
        return block.compose()
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        delegatedConstructorCall.replaceConstructedTypeRef(
            delegatedConstructorCall.constructedTypeRef.transform<FirTypeRef, Nothing?>(this, data).single
        )
        delegatedConstructorCall.transformCalleeReference(this, data)
        return delegatedConstructorCall.compose()
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        annotationCall.transformAnnotationTypeRef(this, data)
        return annotationCall.compose()
    }
}
