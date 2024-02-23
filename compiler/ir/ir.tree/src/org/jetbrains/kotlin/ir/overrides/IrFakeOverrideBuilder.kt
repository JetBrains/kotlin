/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.collectAndFilterRealOverrides
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull

class IrFakeOverrideBuilder(
    private val typeSystem: IrTypeSystemContext,
    private val strategy: FakeOverrideBuilderStrategy,
    private val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) {
    private val overrideChecker = IrOverrideChecker(typeSystem, externalOverridabilityConditions)

    internal data class FakeOverride(val override: IrOverridableMember, val original: IrOverridableMember) {
        override fun toString(): String = override.render()
    }

    private var IrOverridableMember.overriddenSymbols: List<IrSymbol>
        get() = when (this) {
            is IrSimpleFunction -> this.overriddenSymbols
            is IrProperty -> this.overriddenSymbols
            else -> error("Unexpected declaration for overriddenSymbols: $this")
        }
        set(value) {
            when (this) {
                is IrSimpleFunction -> this.overriddenSymbols =
                    value.memoryOptimizedMap { it as? IrSimpleFunctionSymbol ?: error("Unexpected function overridden symbol: $it") }
                is IrProperty -> {
                    val overriddenProperties =
                        value.memoryOptimizedMap { it as? IrPropertySymbol ?: error("Unexpected property overridden symbol: $it") }
                    this.getter?.let { getter ->
                        getter.overriddenSymbols = overriddenProperties.memoryOptimizedMapNotNull { it.owner.getter?.symbol }
                    }
                    this.setter?.let { setter ->
                        setter.overriddenSymbols = overriddenProperties.memoryOptimizedMapNotNull { it.owner.setter?.symbol }
                    }
                    this.overriddenSymbols = overriddenProperties
                }
                else -> error("Unexpected declaration for overriddenSymbols: $this")
            }
        }

    /**
     * This function builds all fake overrides for [clazz] and computes overridden symbols for all its members.
     */
    fun buildFakeOverridesForClass(clazz: IrClass, oldSignatures: Boolean) {
        strategy.inFile(clazz.fileOrNull) {
            val (staticMembers, instanceMembers) =
                clazz.declarations.filterIsInstance<IrOverridableMember>().partition { it.isStaticMember }

            buildFakeOverridesForClassImpl(clazz, instanceMembers, oldSignatures, clazz.superTypes) { !it.isStaticMember }

            // Static Java members from the superclass need fake overrides in the subclass, to support the case when the static member is
            // declared in an inaccessible grandparent class but is exposed as public in the parent. For example:
            //
            //     class A { public static void f() {} }
            //     public class B extends A {}
            //
            // `A.f` is inaccessible from another package, but `B.f` is accessible from everywhere because Java doesn't have the
            // "exposed visibility" error. Accessing the method via the class A would result in an IllegalAccessError at runtime, thus
            // we need to generate a fake override in class B. This is only possible in case of superclasses, as static _interface_ members
            // are not inherited (see JLS 8.4.8 and 9.4.1).
            val superClass = clazz.superTypes.filter { it.classOrFail.owner.isClass }
            buildFakeOverridesForClassImpl(clazz, staticMembers, oldSignatures, superClass) { it.isStaticMember }
        }
    }

    private fun buildFakeOverridesForClassImpl(
        clazz: IrClass,
        allFromCurrent: List<IrOverridableMember>,
        oldSignatures: Boolean,
        supertypes: List<IrType>,
        declarationFilter: (IrOverridableMember) -> Boolean,
    ) {
        val allFromSuper = supertypes.flatMap { superType ->
            superType.classOrFail.owner.declarations
                .filterIsInstanceAnd<IrOverridableMember>(declarationFilter)
                .mapNotNull {
                    val fakeOverride = strategy.fakeOverrideMember(superType, it, clazz) ?: return@mapNotNull null
                    FakeOverride(fakeOverride, it)
                }
        }

        val allFromSuperByName = allFromSuper.groupBy { it.override.name }
        val allFromCurrentByName = allFromCurrent.groupBy { it.name }

        allFromSuperByName.forEach { (name, superMembers) ->
            generateOverridesInFunctionGroup(
                superMembers,
                allFromCurrentByName[name] ?: emptyList(),
                clazz,
                oldSignatures
            )
        }

    }

    /**
     * This function builds all missing fake overrides, assuming that already existing members have correct overriden symbols.
     *
     * In particular, if a member of super class can be overridden, but none of the members have it in their overriddenSymbols,
     * fake override would be created.
     */
    fun buildFakeOverridesForClassUsingOverriddenSymbols(
        clazz: IrClass,
        implementedMembers: List<IrOverridableMember> = emptyList(),
        compatibilityMode: Boolean,
        ignoredParentSymbols: List<IrSymbol> = emptyList()
    ): List<IrOverridableMember> {
        val overriddenMembers = (clazz.declarations.filterIsInstance<IrOverridableMember>() + implementedMembers)
            .flatMap { member -> member.overriddenSymbols.map { it.owner } }
            .toSet()

        val unoverriddenSuperMembers = clazz.superTypes.flatMap { superType ->
            val superClass = superType.getClass() ?: error("Unexpected super type: $superType")
            superClass.declarations
                .filterIsInstanceAnd<IrOverridableMember> {
                    it !in overriddenMembers && it.symbol !in ignoredParentSymbols && !it.isStaticMember
                }
                .mapNotNull { overriddenMember ->
                    val fakeOverride = strategy.fakeOverrideMember(superType, overriddenMember, clazz) ?: return@mapNotNull null
                    FakeOverride(fakeOverride, overriddenMember)
                }
        }

        val unoverriddenSuperMembersGroupedByName = unoverriddenSuperMembers.groupBy { it.override.name }
        val fakeOverrides = mutableListOf<IrOverridableMember>()
        for (group in unoverriddenSuperMembersGroupedByName.values) {
            createAndBindFakeOverrides(clazz, group, fakeOverrides, compatibilityMode)
        }
        return fakeOverrides
    }

    private val IrOverridableMember.isStaticMember: Boolean
        get() = when (this) {
            is IrFunction ->
                dispatchReceiverParameter == null
            is IrProperty ->
                backingField?.isStatic == true ||
                        getter?.let { it.dispatchReceiverParameter == null } == true
            else -> error("Unknown overridable member: ${render()}")
        }

    private fun generateOverridesInFunctionGroup(
        membersFromSupertypes: List<FakeOverride>,
        membersFromCurrent: List<IrOverridableMember>,
        current: IrClass,
        compatibilityMode: Boolean
    ) {
        val notOverridden = membersFromSupertypes.toMutableSet()

        for (fromCurrent in membersFromCurrent) {
            val bound = extractAndBindOverridesForMember(fromCurrent, membersFromSupertypes)
            notOverridden -= bound
        }

        val addedFakeOverrides = mutableListOf<IrOverridableMember>()
        createAndBindFakeOverrides(current, notOverridden, addedFakeOverrides, compatibilityMode)
        current.declarations.addAll(addedFakeOverrides)
    }

    private fun extractAndBindOverridesForMember(
        fromCurrent: IrOverridableMember,
        membersFromSuper: List<FakeOverride>
    ): List<FakeOverride> {
        val bound = ArrayList<FakeOverride>(membersFromSuper.size)
        val overridden = mutableSetOf<FakeOverride>()

        for (fromSupertype in membersFromSuper) {
            // Note: We do allow overriding multiple FOs at once one of which is `isInline=true`.
            val overridability = overrideChecker.isOverridableBy(
                MemberWithOriginal(fromSupertype),
                MemberWithOriginal(fromCurrent),
                checkIsInlineFlag = true,
            )
            when (overridability.result) {
                OverrideCompatibilityInfo.Result.OVERRIDABLE -> {
                    overridden += fromSupertype
                    bound += fromSupertype
                }
                OverrideCompatibilityInfo.Result.CONFLICT -> {
                    bound += fromSupertype
                }
                OverrideCompatibilityInfo.Result.INCOMPATIBLE -> Unit
            }
        }

        // because of binary incompatible changes, it's possible to have private member colliding with fake override
        // In that case we shouldn't generate fake override, but also shouldn't mark them as overridden
        if (!DescriptorVisibilities.isPrivate(fromCurrent.visibility)) {
            fromCurrent.overriddenSymbols = overridden.memoryOptimizedMap { it.original.symbol }
        }

        return bound
    }

    // Based on findMemberWithMaxVisibility from VisibilityUtil.kt.
    private fun findMemberWithMaxVisibility(members: Collection<FakeOverride>): FakeOverride {
        assert(members.isNotEmpty())

        var member: FakeOverride? = null
        for (candidate in members) {
            if (member == null) {
                member = candidate
                continue
            }

            val result = DescriptorVisibilities.compare(member.override.visibility, candidate.override.visibility)
            if (result != null && result < 0) {
                member = candidate
            }
        }
        return member ?: error("Could not find a visible member")
    }

    private fun createAndBindFakeOverrides(
        current: IrClass,
        notOverridden: Collection<FakeOverride>,
        addedFakeOverrides: MutableList<IrOverridableMember>,
        compatibilityMode: Boolean
    ) {
        val fromSuper = notOverridden.toMutableSet()
        while (fromSuper.isNotEmpty()) {
            val notOverriddenFromSuper = filterOutCustomizedFakeOverrides(fromSuper)
            val overridables = extractMembersOverridableInBothWays(
                notOverriddenFromSuper.first(),
                fromSuper
            )
            createAndBindFakeOverride(overridables, current, addedFakeOverrides, compatibilityMode)
        }
    }

    /**
     * If there is a mix of [IrOverridableMember]s with origin=[IrDeclarationOrigin.FAKE_OVERRIDE]s (true "fake overrides")
     * and [IrOverridableMember]s that were customized with the help of [IrUnimplementedOverridesStrategy] (customized "fake overrides"),
     * then leave only true ones. Rationale: They should point to non-abstract callable members in one of super classes, so
     * effectively they are implemented in the current class.
     */
    private fun filterOutCustomizedFakeOverrides(overridableMembers: Collection<FakeOverride>): Collection<FakeOverride> {
        if (overridableMembers.size < 2) return overridableMembers

        val (trueFakeOverrides, customizedFakeOverrides) = overridableMembers.partition { it.override.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
        return trueFakeOverrides.ifEmpty { customizedFakeOverrides }
    }

    private fun determineModalityForFakeOverride(
        members: List<FakeOverride>,
    ): Modality {
        // Optimization: avoid creating hash sets in frequent cases when modality can be computed trivially
        var hasOpen = false
        var hasAbstract = false
        for (member in members) {
            when (member.override.modality) {
                Modality.FINAL -> return Modality.FINAL
                Modality.SEALED -> throw IllegalStateException("Member cannot have SEALED modality: $member")
                Modality.OPEN -> hasOpen = true
                Modality.ABSTRACT -> hasAbstract = true
            }
        }

        if (hasOpen && !hasAbstract) {
            return Modality.OPEN
        }
        if (!hasOpen && hasAbstract) {
            return Modality.ABSTRACT
        }

        val realOverrides = members
            .map { it.original }
            .collectAndFilterRealOverrides()
        return getMinimalModality(realOverrides)
    }

    private fun getMinimalModality(
        members: Collection<IrOverridableMember>,
    ): Modality {
        var result = Modality.ABSTRACT
        for (member in members) {
            val effectiveModality = member.modality
            if (effectiveModality < result) {
                result = effectiveModality
            }
        }
        return result
    }

    private fun IrSimpleFunction.updateAccessorModalityAndVisibility(
        newModality: Modality,
        newVisibility: DescriptorVisibility
    ): IrSimpleFunction? {
        require(this is IrFunctionWithLateBinding) {
            "Unexpected fake override accessor kind: $this"
        }
        // For descriptors it gets INVISIBLE_FAKE.
        if (DescriptorVisibilities.isPrivate(this.visibility)) return null

        this.visibility = newVisibility
        this.modality = newModality
        return this
    }

    private fun createAndBindFakeOverride(
        overridables: List<FakeOverride>,
        currentClass: IrClass,
        addedFakeOverrides: MutableList<IrOverridableMember>,
        compatibilityMode: Boolean
    ) {
        val modality = determineModalityForFakeOverride(overridables)
        val visibility = findMemberWithMaxVisibility(overridables).override.visibility
        val mostSpecific = selectMostSpecificMember(overridables)

        val fakeOverride = mostSpecific.override.apply {
            when (this) {
                is IrPropertyWithLateBinding -> {
                    this.visibility = visibility
                    this.modality = modality
                    this.getter = this.getter?.updateAccessorModalityAndVisibility(modality, visibility)
                    this.setter = this.setter?.updateAccessorModalityAndVisibility(modality, visibility)
                }
                is IrFunctionWithLateBinding -> {
                    this.visibility = visibility
                    this.modality = modality
                }
                else -> error("Unexpected fake override kind: $this")
            }
        }

        fakeOverride.overriddenSymbols = overridables.memoryOptimizedMap { it.original.symbol }

        require(
            fakeOverride.overriddenSymbols.isNotEmpty()
        ) { "Overridden symbols should be set for fake override ${fakeOverride.render()}" }

        addedFakeOverrides.add(fakeOverride)
        strategy.linkFakeOverride(fakeOverride, compatibilityMode)
        strategy.postProcessGeneratedFakeOverride(fakeOverride, currentClass)
    }

    private fun isReturnTypeIsSubtypeOfOtherReturnType(
        a: IrOverridableMember,
        b: IrOverridableMember,
    ): Boolean {
        val typeCheckerState = createIrTypeCheckerState(
            IrTypeSystemContextWithAdditionalAxioms(typeSystem, a.typeParameters, b.typeParameters)
        )
        return AbstractTypeChecker.isSubtypeOf(typeCheckerState, a.returnType, b.returnType)
    }

    private fun isMoreSpecific(
        a: IrOverridableMember,
        b: IrOverridableMember
    ): Boolean {
        return a > b
    }

    // Based on compareTo from FirOverrideService.kt
    private operator fun IrOverridableMember.compareTo(other: IrOverridableMember): Int {
        fun merge(preferA: Boolean, preferB: Boolean, previous: Int): Int = when {
            preferA == preferB -> previous
            preferA && previous >= 0 -> 1
            preferB && previous <= 0 -> -1
            else -> 0
        }

        val aIr = this@compareTo
        val bIr = other
        val byVisibility = DescriptorVisibilities.compare(aIr.visibility, bIr.visibility) ?: 0
        val aReturnType = aIr.returnType
        val bReturnType = bIr.returnType

        val aSubtypesB = isReturnTypeIsSubtypeOfOtherReturnType(aIr, bIr)
        val bSubtypesA = isReturnTypeIsSubtypeOfOtherReturnType(bIr, aIr)

        val byVisibilityAndType = when {
            // Could be that one of them is flexible, in which case the types are not equal but still subtypes of one another;
            // make the inflexible one more specific.
            aSubtypesB && bSubtypesA -> merge(!aReturnType.isFlexible(), !bReturnType.isFlexible(), byVisibility)
            aSubtypesB && byVisibility >= 0 -> 1
            bSubtypesA && byVisibility <= 0 -> -1
            else -> 0
        }

        return when (aIr) {
            is IrSimpleFunction -> byVisibilityAndType
            is IrProperty -> {
                require(bIr is IrProperty)
                val settersComparison = if (aIr.isVar && bIr.isVar) {
                    val aSetter = aIr.setter
                    val bSetter = bIr.setter
                    when {
                        aSetter != null && bSetter != null -> aSetter.compareTo(bSetter)
                        else -> 0
                    }
                } else {
                    0
                }
                val bySetters = merge(
                    preferA = settersComparison >= 0,
                    preferB = settersComparison <= 0,
                    byVisibilityAndType
                )
                merge(aIr.isVar, bIr.isVar, bySetters)
            }
            else -> error("Unexpected type: $aIr")
        }
    }

    private fun IrType.isFlexible(): Boolean {
        return with(typeSystem) { isFlexible() }
    }

    private fun isMoreSpecificThenAllOf(
        candidate: FakeOverride,
        overrides: Collection<FakeOverride>
    ): Boolean {
        // NB subtyping relation in Kotlin is not transitive in presence of flexible types:
        //  String? <: String! <: String, but not String? <: String
        for (override in overrides) {
            if (!isMoreSpecific(candidate.override, override.override)) {
                return false
            }
        }
        return true
    }

    private fun selectMostSpecificMember(overridables: Collection<FakeOverride>): FakeOverride {
        require(!overridables.isEmpty()) { "Should have at least one overridable member" }
        if (overridables.size == 1) {
            return overridables.first()
        }
        val candidates = mutableListOf<FakeOverride>()
        var transitivelyMostSpecific = overridables.first()
        val transitivelyMostSpecificMember = transitivelyMostSpecific
        for (overridable in overridables) {
            if (isMoreSpecificThenAllOf(overridable, overridables)
            ) {
                candidates.add(overridable)
            }
            if (isMoreSpecific(
                    overridable.override,
                    transitivelyMostSpecificMember.override
                )
                && !isMoreSpecific(
                    transitivelyMostSpecificMember.override,
                    overridable.override
                )
            ) {
                transitivelyMostSpecific = overridable
            }
        }
        if (candidates.isEmpty()) {
            return transitivelyMostSpecific
        } else if (candidates.size == 1) {
            return candidates.first()
        }
        var firstNonFlexible: FakeOverride? = null
        for (candidate in candidates) {
            if (candidate.override.returnType !is IrDynamicType) {
                firstNonFlexible = candidate
                break
            }
        }
        return firstNonFlexible ?: candidates.first()
    }

    private fun extractMembersOverridableInBothWays(
        overrider: FakeOverride,
        extractFrom: MutableSet<FakeOverride>
    ): List<FakeOverride> {
        val overridable = arrayListOf<FakeOverride>()
        overridable.add(overrider)
        val iterator = extractFrom.iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (overrider === candidate) {
                iterator.remove()
                continue
            }
            val finalResult = overrideChecker.getBothWaysOverridability(MemberWithOriginal(overrider), MemberWithOriginal(candidate))
            if (finalResult == OverrideCompatibilityInfo.Result.OVERRIDABLE) {
                overridable.add(candidate)
                iterator.remove()
            } else if (finalResult == OverrideCompatibilityInfo.Result.CONFLICT) {
                iterator.remove()
            }
        }
        return overridable
    }
}

private val IrOverridableMember.typeParameters: List<IrTypeParameter>
    get() = when (this) {
        is IrSimpleFunction -> typeParameters
        is IrProperty -> getter?.typeParameters.orEmpty()
        else -> error("Unexpected type of declaration: ${this::class.java}, $this")
    }

private val IrOverridableMember.returnType: IrType
    get() = when (this) {
        is IrSimpleFunction -> returnType
        is IrProperty -> getter!!.returnType
        else -> error("Unexpected type of declaration: ${this::class.java}, $this")
    }
