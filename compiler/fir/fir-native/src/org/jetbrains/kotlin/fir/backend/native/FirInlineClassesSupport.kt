/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.native

import org.jetbrains.kotlin.backend.konan.InlineClassesSupport
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(SymbolInternals::class)
class FirInlineClassesSupport(private val session: FirSession) : InlineClassesSupport<FirClass, ConeKotlinType>() {
    override fun isNullable(type: ConeKotlinType): Boolean {
        return type.isMarkedNullable
    }

    override fun makeNullable(type: ConeKotlinType): ConeKotlinType {
        return type.withNullability(nullable = true, session.typeContext)
    }

    override fun erase(type: ConeKotlinType): FirClass {
        val expandedType = type.fullyExpandedType(session)
        return when (expandedType) {
            is ConeClassLikeType -> {
                val symbol = expandedType.lookupTag.toRegularClassSymbol(session)
                    ?: error("Cannot find class for ${expandedType.lookupTag}")
                symbol.fir
            }
            is ConeTypeParameterType -> {
                // For type parameters, we need to look at their bounds
                val symbol = expandedType.lookupTag.symbol
                val bounds = symbol.resolvedBounds
                val firstBound = bounds.firstOrNull()?.coneType ?: session.builtinTypes.anyType.coneType
                erase(firstBound)
            }
            else -> error("Unexpected type: $expandedType")
        }
    }

    override fun computeFullErasure(type: ConeKotlinType): Sequence<FirClass> {
        val expandedType = type.fullyExpandedType(session)
        return when (expandedType) {
            is ConeClassLikeType -> {
                val symbol = expandedType.lookupTag.toRegularClassSymbol(session)
                    ?: error("Cannot find class for ${expandedType.lookupTag}")
                sequenceOf(symbol.fir)
            }
            is ConeTypeParameterType -> {
                val symbol = expandedType.lookupTag.symbol
                symbol.resolvedBounds.asSequence()
                    .flatMap { computeFullErasure(it.coneType) }
                    .ifEmpty { sequenceOf(erase(session.builtinTypes.anyType.coneType)) }
            }
            else -> error("Unexpected type: $expandedType")
        }
    }

    override fun hasInlineModifier(clazz: FirClass): Boolean {
        return clazz is FirRegularClass && clazz.isInlineOrValue
    }

    override fun getNativePointedSuperclass(clazz: FirClass): FirClass? {
        if (clazz !is FirRegularClass) return null
        
        var currentClass: FirRegularClass? = clazz
        while (currentClass != null) {
            if (currentClass.symbol.classId.asSingleFqName() == InteropFqNames.nativePointed) {
                return currentClass
            }
            
            val superTypes = currentClass.superConeTypes
            currentClass = superTypes.firstOrNull()?.toRegularClassSymbol(session)?.fir
        }
        
        return null
    }

    override fun getInlinedClassUnderlyingType(clazz: FirClass): ConeKotlinType {
        require(clazz is FirRegularClass) { "Expected FirRegularClass but got ${clazz::class.simpleName}" }
        
        // First try to get from inline class representation (for already resolved classes)
        clazz.inlineClassRepresentation?.underlyingType?.let { return it }
        
        // Otherwise, get from primary constructor parameter
        val primaryConstructor = clazz.primaryConstructorIfAny(session)?.fir
        val parameter = primaryConstructor?.valueParameters?.singleOrNull()
            ?: error("Inline class $clazz should have exactly one value parameter")
        
        return parameter.returnTypeRef.coneType
    }

    override fun getPackageFqName(clazz: FirClass): FqName? {
        return clazz.symbol.classId.packageFqName.takeUnless { it.isRoot }
    }

    override fun getName(clazz: FirClass): Name? {
        return clazz.symbol.classId.shortClassName.takeUnless { it.isSpecial }
    }

    override fun isTopLevelClass(clazz: FirClass): Boolean {
        return !clazz.symbol.classId.isNestedClass
    }
}