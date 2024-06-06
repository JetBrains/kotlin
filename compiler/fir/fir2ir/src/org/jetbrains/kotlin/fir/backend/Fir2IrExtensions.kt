/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.utils.InjectedValue
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.util.SymbolTable

interface Fir2IrExtensions {
    val irNeedsDeserialization: Boolean

    /**
     * Determines if parameters of tailrec functions should be generated with isAssignable = true
     */
    val parametersAreAssignable: Boolean

    val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>

    fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean
    fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue?

    /**
     * Platform-dependent logic to determine whether a backing field is required for [property].
     * Should be called instead of [FirProperty.hasBackingField] to decide whether to create a backing field.
     * The implementation should return `true` in case a platform-dependent condition for backing field existence is met,
     * otherwise it should return the result of [Fir2IrExtensions.Default.hasBackingField].
     */
    fun hasBackingField(property: FirProperty, session: FirSession): Boolean

    /**
     * Whether this declaration is forcibly made static in the sense that it has no dispatch receiver.
     *
     * For example, on JVM this corresponds to the [JvmStatic] annotation.
     */
    fun isTrueStatic(declaration: FirCallableDeclaration, session: FirSession): Boolean

    fun initializeIrBuiltInsAndSymbolTable(irBuiltIns: IrBuiltIns, symbolTable: SymbolTable)

    fun shouldGenerateDelegatedMember(delegateMemberCandidate: IrOverridableDeclaration<*>): Boolean

    object Default : Fir2IrExtensions {
        override val irNeedsDeserialization: Boolean
            get() = false

        override val parametersAreAssignable: Boolean
            get() = false

        override val externalOverridabilityConditions: List<IrExternalOverridabilityCondition> = emptyList()
        override fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean = false
        override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): Nothing? = null
        override fun hasBackingField(property: FirProperty, session: FirSession): Boolean = property.hasBackingField
        override fun isTrueStatic(declaration: FirCallableDeclaration, session: FirSession): Boolean = false
        override fun initializeIrBuiltInsAndSymbolTable(irBuiltIns: IrBuiltIns, symbolTable: SymbolTable) {}
        override fun shouldGenerateDelegatedMember(delegateMemberCandidate: IrOverridableDeclaration<*>): Boolean = true
    }
}
