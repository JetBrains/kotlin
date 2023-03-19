/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

public class ClassBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?,
    private val classId: ClassId,
    private val classKind: ClassKind,
) : DeclarationBuildingContext<FirRegularClass>(session, key, owner) {
    private val superTypeProviders = mutableListOf<(List<FirTypeParameterRef>) -> ConeKotlinType>()

    /**
     * Adds [type] as supertype for constructed class
     *
     * If no supertypes are declared [kotlin.Any] supertype will be
     *   added automatically
     */
    public fun superType(type: ConeKotlinType) {
        superTypeProviders += { type }
    }

    /**
     * Adds type created by [typeProvider] as supertype for constructed class
     * Use this overload when supertype uses type parameters of constructed class
     *
     * If no supertypes are declared [kotlin.Any] supertype will be
     *   added automatically
     */
    public fun superType(typeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType) {
        superTypeProviders += typeProvider
    }

    override fun build(): FirRegularClass {
        return buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin
            classKind = this@ClassBuildingContext.classKind
            scopeProvider = session.kotlinScopeProvider
            status = generateStatus()
            name = classId.shortClassName
            symbol = FirRegularClassSymbol(classId)

            if (status.isInner) {
                requireNotNull(owner) { "Inner class must have owner" }
                owner.typeParameterSymbols.mapTo(typeParameters) { buildOuterClassTypeParameterRef { symbol = it } }
            }
            val ownParameters = this@ClassBuildingContext.typeParameters.map {
                generateTypeParameter(it, symbol)
            }
            typeParameters += ownParameters
            initTypeParameterBounds(typeParameters, ownParameters)

            if (superTypeProviders.isEmpty()) {
                superTypeRefs += session.builtinTypes.anyType
            } else {
                superTypeProviders.mapTo(this.superTypeRefs) {
                    buildResolvedTypeRef { type = it(this@buildRegularClass.typeParameters) }
                }
            }
        }.apply {
            if (owner?.isLocal == true) {
                containingClassForLocalAttr = owner.toLookupTag()
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

/**
 * Creates top-level class with given [classId]
 * All declarations in class should be generated using methods from [FirDeclarationGenerationExtension]
 * Generation of top-level classes with [FirDeclarationsForMetadataProviderExtension] is prohibited
 *
 * If no supertypes added then [kotlin.Any] supertype will be added automatically
 *
 * Created class won't have a constructor; constructor can be added separately with [createConstructor] function
 */
public fun FirExtension.createTopLevelClass(
    classId: ClassId,
    key: GeneratedDeclarationKey,
    classKind: ClassKind = ClassKind.CLASS,
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    return ClassBuildingContext(session, key, owner = null, classId, classKind).apply(config).build()
}

/**
 * Creates nested class for [owner] class with name [name]
 * If class is generated in [FirDeclarationGenerationExtension], all its declarations should be generated
 *   using methods from [FirDeclarationGenerationExtension]
 * If class is generated in [FirDeclarationsForMetadataProviderExtension] all its declarations should be manually added right to
 *   FIR node of created class
 *
 * If no supertypes added then [kotlin.Any] supertype will be added automatically
 *
 * Created class won't have a constructor; constructor can be added separately with [createConstructor] function
 *
 * By default, the class is only nested; to create an inner class, create nested class and add inner status via status()
 */
public fun FirExtension.createNestedClass(
    owner: FirClassSymbol<*>,
    name: Name,
    key: GeneratedDeclarationKey,
    classKind: ClassKind = ClassKind.CLASS,
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    return ClassBuildingContext(session, key, owner, owner.classId.createNestedClassId(name), classKind).apply(config).build()
}

/**
 * Creates companion object for [owner] class
 * If class is generated in [FirDeclarationGenerationExtension], all its declarations should be generated
 *   using methods from [FirDeclarationGenerationExtension]
 * If class is generated in [FirDeclarationsForMetadataProviderExtension] all its declarations should be manually added right to
 *   FIR node of created class
 *
 * If no supertypes added then [kotlin.Any] supertype will be added automatically
 *
 * Created class won't have a constructor; constructor can be added separately with [createDefaultPrivateConstructor] function
 */
public fun FirExtension.createCompanionObject(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    val classId = owner.classId.createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    return ClassBuildingContext(session, key, owner, classId, ClassKind.OBJECT).apply(config).apply {
        modality = Modality.FINAL
        status {
            isCompanion = true
        }
    }.build()
}
