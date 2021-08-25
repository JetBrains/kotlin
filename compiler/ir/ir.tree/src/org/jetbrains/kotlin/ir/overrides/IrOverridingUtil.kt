/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.collectAndFilterRealOverrides
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.incompatible
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.Variance

abstract class FakeOverrideBuilderStrategy {
    open fun fakeOverrideMember(superType: IrType, member: IrOverridableMember, clazz: IrClass): IrOverridableMember =
        buildFakeOverrideMember(superType, member, clazz)

    fun linkFakeOverride(fakeOverride: IrOverridableMember, compatibilityMode: Boolean) {
        when (fakeOverride) {
            is IrFakeOverrideFunction -> linkFunctionFakeOverride(fakeOverride, compatibilityMode)
            is IrFakeOverrideProperty -> linkPropertyFakeOverride(fakeOverride, compatibilityMode)
            else -> error("Unexpected fake override: $fakeOverride")
        }
    }

    protected abstract fun linkFunctionFakeOverride(declaration: IrFakeOverrideFunction, compatibilityMode: Boolean)
    protected abstract fun linkPropertyFakeOverride(declaration: IrFakeOverrideProperty, compatibilityMode: Boolean)
}

private fun IrOverridableMember.isPrivateToThisModule(thisClass: IrClass, memberClass: IrClass): Boolean {
    if (visibility != DescriptorVisibilities.INTERNAL) return false
    val thisModule = thisClass.fileOrNull?.module
    val memberModule = memberClass.fileOrNull?.module
    if (thisModule == memberModule) return false

    //   Note: On WASM backend there is possible if `thisClass` is from `IrExternalPackageFragment` which module is null

    if (thisModule == null || memberModule == null) return false

    return !isInFriendModules(thisModule, memberModule)
}

@Suppress("UNUSED_PARAMETER")
private fun isInFriendModules(thisModule: IrModuleFragment, friendModule: IrModuleFragment): Boolean {
    // TODO: check if [friendModule] is a friend of [thisModule]
    // See: KT-47192

    return false
}

fun buildFakeOverrideMember(superType: IrType, member: IrOverridableMember, clazz: IrClass): IrOverridableMember {
    require(superType is IrSimpleType) { "superType is $superType, expected IrSimpleType" }
    val classifier = superType.classifier
    require(classifier is IrClassSymbol) { "superType classifier is not IrClassSymbol: $classifier" }

    val typeParameters = extractTypeParameters(classifier.owner)
    val superArguments = superType.arguments
    assert(typeParameters.size == superArguments.size) {
        "typeParameters = $typeParameters size != typeArguments = $superArguments size "
    }

    val substitutionMap = mutableMapOf<IrTypeParameterSymbol, IrType>()

    for (i in typeParameters.indices) {
        val tp = typeParameters[i]
        val ta = superArguments[i]
        require(ta is IrTypeProjection) { "Unexpected super type argument: ${ta.render()} @ $i" }
        assert(ta.variance == Variance.INVARIANT) { "Unexpected variance in super type argument: ${ta.variance} @$i" }
        substitutionMap[tp.symbol] = ta.type
    }

    val copier = DeepCopyIrTreeWithSymbolsForFakeOverrides(substitutionMap)
    val deepCopyFakeOverride = copier.copy(member, clazz) as IrOverridableMember
    deepCopyFakeOverride.parent = clazz
    if (deepCopyFakeOverride.isPrivateToThisModule(clazz, classifier.owner))
        deepCopyFakeOverride.visibility = DescriptorVisibilities.INVISIBLE_FAKE

    return deepCopyFakeOverride
}


// TODO:
// The below pile of code is basically half of OverridingUtil.java
// adapted to IR and converted to Kotlin.
// Need to convert frontend's OverridingUtil to Kotlin and merge this codes
// to use abstract overridable member interfaces.

class IrOverridingUtil(
    private val typeSystem: IrTypeSystemContext,
    private val fakeOverrideBuilder: FakeOverrideBuilderStrategy
) {
    private val originals = mutableMapOf<IrOverridableMember, IrOverridableMember>()
    private val IrOverridableMember.original get() = originals[this] ?: error("No original for ${this.render()}")
    private val originalSuperTypes = mutableMapOf<IrOverridableMember, IrType>()

    fun clear() {
        originals.clear()
        originalSuperTypes.clear()
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
                    value.map { it as? IrSimpleFunctionSymbol ?: error("Unexpected function overridden symbol: $it") }
                is IrProperty -> {
                    val overriddenProperties = value.map { it as? IrPropertySymbol ?: error("Unexpected property overridden symbol: $it") }
                    val getter = this.getter ?: error("Property has no getter: ${render()}")
                    getter.overriddenSymbols = overriddenProperties.map { it.owner.getter!!.symbol }
                    this.setter?.let { setter ->
                        setter.overriddenSymbols = overriddenProperties.mapNotNull { it.owner.setter?.symbol }
                    }
                    this.overriddenSymbols = overriddenProperties
                }
                else -> error("Unexpected declaration for overriddenSymbols: $this")
            }
        }

    fun buildFakeOverridesForClass(clazz: IrClass, oldSignatures: Boolean) {
        val superTypes = clazz.superTypes

        @Suppress("UNCHECKED_CAST")
        val fromCurrent = clazz.declarations.filterIsInstance<IrOverridableMember>()

        val allFromSuper = superTypes.flatMap { superType ->
            val superClass = superType.getClass() ?: error("Unexpected super type: $superType")
            superClass.declarations
                .filter { it.isOverridableMemberOrAccessor() }
                .map {
                    val overriddenMember = it as IrOverridableMember
                    val fakeOverride = fakeOverrideBuilder.fakeOverrideMember(superType, overriddenMember, clazz)
                    originals[fakeOverride] = overriddenMember
                    originalSuperTypes[fakeOverride] = superType
                    fakeOverride
                }
        }

        val allFromSuperByName = allFromSuper.groupBy { it.name }

        allFromSuperByName.forEach { group ->
            generateOverridesInFunctionGroup(
                group.value,
                fromCurrent.filter { it.name == group.key },
                clazz, oldSignatures
            )
        }
    }

    fun buildFakeOverridesForClassUsingOverriddenSymbols(
        clazz: IrClass,
        implementedMembers: List<IrOverridableMember> = emptyList(),
        compatibilityMode: Boolean
    ): List<IrOverridableMember> {
        val overriddenMembers = (clazz.declarations.filterIsInstance<IrOverridableMember>() + implementedMembers)
            .flatMap { member -> member.overriddenSymbols.map { it.owner } }
            .toSet()

        val unoverriddenSuperMembers = clazz.superTypes.flatMap { superType ->
            val superClass = superType.getClass() ?: error("Unexpected super type: $superType")
            superClass.declarations
                .filterIsInstance<IrOverridableMember>()
                .filterNot {
                    it in overriddenMembers || it.isStaticMember || DescriptorVisibilities.isPrivate(it.visibility)
                }
                .map { overriddenMember ->
                    val fakeOverride = fakeOverrideBuilder.fakeOverrideMember(superType, overriddenMember, clazz)
                    originals[fakeOverride] = overriddenMember
                    originalSuperTypes[fakeOverride] = superType
                    fakeOverride
                }
        }

        val unoverriddenSuperMembersGroupedByName = unoverriddenSuperMembers.groupBy { it.name }
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
        membersFromSupertypes: List<IrOverridableMember>,
        membersFromCurrent: List<IrOverridableMember>,
        current: IrClass,
        compatibilityMode: Boolean
    ) {
        val notOverridden = membersFromSupertypes.toMutableSet()

        for (fromCurrent in membersFromCurrent) {
            val bound = extractAndBindOverridesForMember(fromCurrent, membersFromSupertypes)
            notOverridden.removeAll(bound)
        }

        val addedFakeOverrides = mutableListOf<IrOverridableMember>()
        createAndBindFakeOverrides(current, notOverridden, addedFakeOverrides, compatibilityMode)
        current.declarations.addAll(addedFakeOverrides)
    }

    private fun extractAndBindOverridesForMember(
        fromCurrent: IrOverridableMember,
        descriptorsFromSuper: Collection<IrOverridableMember>
    ): Collection<IrOverridableMember> {
        val bound = ArrayList<IrOverridableMember>(descriptorsFromSuper.size)
        val overridden = mutableSetOf<IrOverridableMember>()
        for (fromSupertype in descriptorsFromSuper) {
            val result = isOverridableBy(fromSupertype, fromCurrent/*, current*/).result
            val isVisibleForOverride =
                isVisibleForOverride(fromCurrent, fromSupertype.original)
            when (result) {
                OverrideCompatibilityInfo.Result.OVERRIDABLE -> {
                    if (isVisibleForOverride) {
                        overridden.add(fromSupertype)
                    }
                    bound.add(fromSupertype)
                }
                OverrideCompatibilityInfo.Result.CONFLICT -> {
                    // if (isVisibleForOverride) {
                    //     strategy.overrideConflict(fromSupertype, fromCurrent)
                    // }

                    // Do nothing.
                    bound.add(fromSupertype)
                }
                OverrideCompatibilityInfo.Result.INCOMPATIBLE -> {
                }
            }
        }
        //strategy.setOverriddenDescriptors(fromCurrent, overridden)
        fromCurrent.overriddenSymbols = overridden.map { it.original.symbol }

        return bound
    }

    private fun createAndBindFakeOverrides(
        current: IrClass,
        notOverridden: Collection<IrOverridableMember>,
        addedFakeOverrides: MutableList<IrOverridableMember>,
        compatibilityMode: Boolean
    ) {
        val fromSuper = notOverridden.toMutableSet()
        while (fromSuper.isNotEmpty()) {
            val notOverriddenFromSuper = findMemberWithMaxVisibility(fromSuper)
            val overridables = extractMembersOverridableInBothWays(
                notOverriddenFromSuper,
                fromSuper
            )
            createAndBindFakeOverride(overridables, current, addedFakeOverrides, compatibilityMode)
        }
    }

    private fun filterVisibleFakeOverrides(toFilter: Collection<IrOverridableMember>): Collection<IrOverridableMember> {
        return toFilter.filter { member: IrOverridableMember ->
            !DescriptorVisibilities.isPrivate(member.visibility) && member.visibility != DescriptorVisibilities.INVISIBLE_FAKE
        }
    }

    private fun determineModalityForFakeOverride(
        members: Collection<IrOverridableMember>,
        current: IrClass
    ): Modality {
        // Optimization: avoid creating hash sets in frequent cases when modality can be computed trivially
        var hasOpen = false
        var hasAbstract = false
        for (member in members) {
            when (member.modality) {
                Modality.FINAL -> return Modality.FINAL
                Modality.SEALED -> throw IllegalStateException("Member cannot have SEALED modality: $member")
                Modality.OPEN -> hasOpen = true
                Modality.ABSTRACT -> hasAbstract = true
            }
        }

        // Fake overrides of abstract members in non-abstract expected classes should not be abstract, because otherwise it would be
        // impossible to inherit a non-expected class from that expected class in common code.
        // We're making their modality that of the containing class, because this is the least confusing behavior for the users.
        // However, it may cause problems if we reuse resolution results of common code when compiling platform code (see KT-15220)
        val transformAbstractToClassModality =
            current.isExpect && current.modality !== Modality.ABSTRACT && current.modality !== Modality.SEALED
        if (hasOpen && !hasAbstract) {
            return Modality.OPEN
        }
        if (!hasOpen && hasAbstract) {
            return if (transformAbstractToClassModality) current.modality else Modality.ABSTRACT
        }

        val realOverrides = members
            .map { originals[it]!! }
            .collectAndFilterRealOverrides()
        return getMinimalModality(realOverrides, transformAbstractToClassModality, current.modality)
    }

    private fun areEquivalent(a: IrOverridableMember, b: IrOverridableMember) = (a == b)

    fun overrides(f: IrOverridableMember, g: IrOverridableMember): Boolean {

        if (f != g && areEquivalent(f.original, g.original)) return true

        for (overriddenFunction in getOverriddenDeclarations(f)) {
            if (areEquivalent(g.original, overriddenFunction)) return true
        }
        return false
    }

    private fun getOverriddenDeclarations(member: IrOverridableMember): Set<IrOverridableMember> {
        val result = mutableSetOf<IrOverridableMember>()
        collectOverriddenDeclarations(member.original, result)
        return result
    }

    private fun collectOverriddenDeclarations(
        member: IrOverridableMember,
        result: MutableSet<IrOverridableMember>
    ) {
        if (member.isReal) {
            result.add(member)
        } else {
            check(member.overriddenSymbols.isNotEmpty()) { "No overridden descriptors found for (fake override) $member" }
            for (overridden in member.original.overriddenSymbols.map { it.owner as IrOverridableMember }) {
                val original = overridden.original
                collectOverriddenDeclarations(original, result)
                result.add(original)
            }
        }
    }

    private fun getMinimalModality(
        descriptors: Collection<IrOverridableMember>,
        transformAbstractToClassModality: Boolean,
        classModality: Modality
    ): Modality {
        var result = Modality.ABSTRACT
        for (descriptor in descriptors) {
            val effectiveModality =
                if (transformAbstractToClassModality && descriptor.modality === Modality.ABSTRACT) classModality else descriptor.modality
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
        require(this is IrFakeOverrideFunction) {
            "Unexpected fake override accessor kind: $this"
        }
        // For descriptors it gets INVISIBLE_FAKE.
        if (this.visibility == DescriptorVisibilities.PRIVATE) return null

        this.visibility = newVisibility
        this.modality = newModality
        return this
    }

    private fun createAndBindFakeOverride(
        overridables: Collection<IrOverridableMember>,
        current: IrClass,
        addedFakeOverrides: MutableList<IrOverridableMember>,
        compatibilityMode: Boolean
    ) {
        val effectiveOverridden = filterVisibleFakeOverrides(overridables)

        // The descriptor based algorithm goes further building invisible fakes here,
        // but we don't use invisible fakes in IR
        if (effectiveOverridden.isEmpty()) return

        val modality = determineModalityForFakeOverride(effectiveOverridden, current)
        val visibility = findMemberWithMaxVisibility(effectiveOverridden).visibility
        val mostSpecific = selectMostSpecificMember(effectiveOverridden)

        val fakeOverride = mostSpecific.apply {
            when (this) {
                is IrFakeOverrideProperty -> {
                    this.visibility = visibility
                    this.modality = modality
                    this.getter = this.getter?.updateAccessorModalityAndVisibility(modality, visibility)
                    this.setter = this.setter?.updateAccessorModalityAndVisibility(modality, visibility)
                }
                is IrFakeOverrideFunction -> {
                    this.visibility = visibility
                    this.modality = modality
                }
                else -> error("Unexpected fake override kind: $this")
            }
        }

        fakeOverride.overriddenSymbols = effectiveOverridden.map { it.original.symbol }

        assert(
            fakeOverride.overriddenSymbols.isNotEmpty()
        ) { "Overridden symbols should be set for " + CallableMemberDescriptor.Kind.FAKE_OVERRIDE }

        addedFakeOverrides.add(fakeOverride)
        fakeOverrideBuilder.linkFakeOverride(fakeOverride, compatibilityMode)
    }

    private fun isVisibilityMoreSpecific(
        a: IrOverridableMember,
        b: IrOverridableMember
    ): Boolean {
        val result =
            DescriptorVisibilities.compare(a.visibility, b.visibility)
        return result == null || result >= 0
    }

    private fun isAccessorMoreSpecific(
        a: IrSimpleFunction?,
        b: IrSimpleFunction?
    ): Boolean {
        return if (a == null || b == null) true else isVisibilityMoreSpecific(a, b)
    }

    private fun IrTypeCheckerState.isSubtypeOf(a: IrType, b: IrType) =
        AbstractTypeChecker.isSubtypeOf(this as TypeCheckerState, a, b)

    private fun IrTypeCheckerState.equalTypes(a: IrType, b: IrType) =
        AbstractTypeChecker.equalTypes(this as TypeCheckerState, a, b)

    private fun createTypeCheckerState(a: List<IrTypeParameter>, b: List<IrTypeParameter>) =
        IrTypeCheckerState(IrTypeSystemContextWithAdditionalAxioms(typeSystem, a, b))

    private fun isReturnTypeMoreSpecific(
        a: IrOverridableMember,
        aReturnType: IrType,
        b: IrOverridableMember,
        bReturnType: IrType
    ): Boolean {
        val typeCheckerContext = createTypeCheckerState(a.typeParameters, b.typeParameters)
        return typeCheckerContext.isSubtypeOf(aReturnType, bReturnType)
    }

    private fun isMoreSpecific(
        a: IrOverridableMember,
        b: IrOverridableMember
    ): Boolean {
        val aReturnType = a.returnType
        val bReturnType = b.returnType
        if (!isVisibilityMoreSpecific(a, b)) return false
        if (a is IrSimpleFunction) {
            assert(b is IrSimpleFunction) { "b is " + b.javaClass }
            return isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType)
        }
        if (a is IrProperty) {
            assert(b is IrProperty) { "b is " + b.javaClass }
            val pa = a
            val pb = b as IrProperty
            if (!isAccessorMoreSpecific(
                    pa.setter,
                    pb.setter
                )
            ) return false
            return if (pa.isVar && pb.isVar) {
                createTypeCheckerState(
                    a.getter!!.typeParameters,
                    b.getter!!.typeParameters
                ).equalTypes(aReturnType, bReturnType)
            } else {
                // both vals or var vs val: val can't be more specific then var
                !(!pa.isVar && pb.isVar) && isReturnTypeMoreSpecific(
                    a, aReturnType,
                    b, bReturnType
                )
            }
        }
        error("Unexpected callable: $a")
    }

    private fun isMoreSpecificThenAllOf(
        candidate: IrOverridableMember,
        descriptors: Collection<IrOverridableMember>
    ): Boolean {
        // NB subtyping relation in Kotlin is not transitive in presence of flexible types:
        //  String? <: String! <: String, but not String? <: String
        for (descriptor in descriptors) {
            if (!isMoreSpecific(candidate, descriptor)) {
                return false
            }
        }
        return true
    }

    private fun selectMostSpecificMember(
        overridables: Collection<IrOverridableMember>
    ): IrOverridableMember {
        assert(!overridables.isEmpty()) { "Should have at least one overridable descriptor" }
        if (overridables.size == 1) {
            return overridables.first()
        }
        val candidates = mutableListOf<IrOverridableMember>()
        var transitivelyMostSpecific = overridables.first()
        val transitivelyMostSpecificDescriptor = transitivelyMostSpecific
        for (overridable in overridables) {
            if (isMoreSpecificThenAllOf(overridable, overridables)
            ) {
                candidates.add(overridable)
            }
            if (isMoreSpecific(
                    overridable,
                    transitivelyMostSpecificDescriptor
                )
                && !isMoreSpecific(
                    transitivelyMostSpecificDescriptor,
                    overridable
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
        var firstNonFlexible: IrOverridableMember? = null
        for (candidate in candidates) {
            if (candidate.returnType !is IrDynamicType) {
                firstNonFlexible = candidate
                break
            }
        }
        return firstNonFlexible ?: candidates.first()
    }

    private fun extractMembersOverridableInBothWays(
        overrider: IrOverridableMember,
        extractFrom: MutableCollection<IrOverridableMember>
    ): Collection<IrOverridableMember> {
        val overridable = arrayListOf<IrOverridableMember>()
        overridable.add(overrider)
        val iterator = extractFrom.iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (overrider === candidate) {
                iterator.remove()
                continue
            }
            val finalResult =
                getBothWaysOverridability(
                    overrider,
                    candidate
                )
            if (finalResult == OverrideCompatibilityInfo.Result.OVERRIDABLE) {
                overridable.add(candidate)
                iterator.remove()
            } else if (finalResult == OverrideCompatibilityInfo.Result.CONFLICT) {
                iterator.remove()
            }
        }
        return overridable
    }

    private fun getBothWaysOverridability(
        overriderDescriptor: IrOverridableMember,
        candidateDescriptor: IrOverridableMember
    ): OverrideCompatibilityInfo.Result {
        val result1 = isOverridableBy(
            candidateDescriptor,
            overriderDescriptor
            //null
        ).result
        val result2 = isOverridableBy(
            overriderDescriptor,
            candidateDescriptor
            //null
        ).result
        return if (result1 == OverrideCompatibilityInfo.Result.OVERRIDABLE && result2 == OverrideCompatibilityInfo.Result.OVERRIDABLE)
            OverrideCompatibilityInfo.Result.OVERRIDABLE
        else if (result1 == OverrideCompatibilityInfo.Result.CONFLICT || result2 == OverrideCompatibilityInfo.Result.CONFLICT)
            OverrideCompatibilityInfo.Result.CONFLICT
        else
            OverrideCompatibilityInfo.Result.INCOMPATIBLE
    }

    private fun isOverridableBy(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
        // subClass: IrClass?
    ): OverrideCompatibilityInfo {
        return isOverridableBy(superMember, subMember/*, subClass*/, false)
    }

    private fun isOverridableBy(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
        // subClass: IrClass?, Would only be needed for external overridability conditions.
        checkReturnType: Boolean
    ): OverrideCompatibilityInfo {
        val basicResult = isOverridableByWithoutExternalConditions(superMember, subMember, checkReturnType)
        return if (basicResult.result == OverrideCompatibilityInfo.Result.OVERRIDABLE)
            OverrideCompatibilityInfo.success()
        else
            basicResult
        // The frontend goes into external overridability condition details here, but don't deal with them in IR (yet?).
    }

    private val IrOverridableMember.compiledValueParameters
        get() = when (this) {
            is IrSimpleFunction -> extensionReceiverParameter?.let { listOf(it) + valueParameters } ?: valueParameters
            is IrProperty -> getter!!.extensionReceiverParameter?.let { listOf(it) } ?: emptyList()
            else -> error("Unexpected declaration for compiledValueParameters: $this")
        }

    private val IrOverridableMember.returnType
        get() = when (this) {
            is IrSimpleFunction -> this.returnType
            is IrProperty -> this.getter!!.returnType
            else -> error("Unexpected declaration for returnType: $this")
        }

    private val IrOverridableMember.typeParameters
        get() = when (this) {
            is IrSimpleFunction -> this.typeParameters
            is IrProperty -> this.getter!!.typeParameters
            else -> error("Unexpected declaration for typeParameters: $this")
        }

    private fun isOverridableByWithoutExternalConditions(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember,
        checkReturnType: Boolean
    ): OverrideCompatibilityInfo {
        val basicOverridability = getBasicOverridabilityProblem(superMember, subMember)
        if (basicOverridability != null) return basicOverridability

        val superValueParameters = superMember.compiledValueParameters
        val subValueParameters = subMember.compiledValueParameters
        val superTypeParameters = superMember.typeParameters
        val subTypeParameters = subMember.typeParameters

        if (superTypeParameters.size != subTypeParameters.size) {
            /* TODO: do we need this in IR?
            superValueParameters.forEachIndexed { index, superParameter ->
                if (!AbstractTypeChecker.equalTypes(
                        defaultTypeCheckerContext as AbstractTypeCheckerContext,
                        superParameter.type,
                        subValueParameters[index].type
                    )
                ) {
                    return OverrideCompatibilityInfo.incompatible("Type parameter number mismatch")
                }
            }
            return OverrideCompatibilityInfo.conflict("Type parameter number mismatch")
            */

            return incompatible("Type parameter number mismatch")
        }

        val typeCheckerState =
            IrTypeCheckerState(
                IrTypeSystemContextWithAdditionalAxioms(
                    typeSystem,
                    superTypeParameters,
                    subTypeParameters
                )
            )

        /* TODO: check the bounds. See OverridingUtil.areTypeParametersEquivalent()
        superTypeParameters.forEachIndexed { index, parameter ->
            if (!AbstractTypeChecker.areTypeParametersEquivalent(
                    typeCheckerContext as AbstractTypeCheckerContext,
                    subTypeParameters[index].type,
                    parameter.type
                )
            ) return OverrideCompatibilityInfo.incompatible("Type parameter bounds mismatch")
        }
        */

        assert(superValueParameters.size == subValueParameters.size)

        superValueParameters.forEachIndexed { index, parameter ->
            if (!AbstractTypeChecker.equalTypes(
                    typeCheckerState as TypeCheckerState,
                    subValueParameters[index].type,
                    parameter.type
                )
            ) return incompatible("Value parameter type mismatch")
        }

        if (superMember is IrSimpleFunction && subMember is IrSimpleFunction && superMember.isSuspend != subMember.isSuspend) {
            return OverrideCompatibilityInfo.conflict("Incompatible suspendability")
        }

        if (checkReturnType) {
            if (!AbstractTypeChecker.isSubtypeOf(
                    typeCheckerState as TypeCheckerState,
                    subMember.returnType,
                    superMember.returnType
                )
            ) return OverrideCompatibilityInfo.conflict("Return type mismatch")
        }
        return OverrideCompatibilityInfo.success()
    }

    private fun getBasicOverridabilityProblem(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember
    ): OverrideCompatibilityInfo? {
        if (superMember is IrSimpleFunction && subMember !is IrSimpleFunction ||
            superMember is IrProperty && subMember !is IrProperty
        ) {
            return incompatible("Member kind mismatch")
        }
        require((superMember is IrSimpleFunction || superMember is IrProperty)) {
            "This type of IrDeclaration cannot be checked for overridability: $superMember"
        }

        return if (superMember.name != subMember.name) {
            incompatible("Name mismatch")
        } else
            checkReceiverAndParameterCount(superMember, subMember)
    }

    private fun checkReceiverAndParameterCount(
        superMember: IrOverridableMember,
        subMember: IrOverridableMember
    ): OverrideCompatibilityInfo? {
        return when (superMember) {
            is IrSimpleFunction -> {
                require(subMember is IrSimpleFunction)
                when {
                    superMember.extensionReceiverParameter == null != (subMember.extensionReceiverParameter == null) -> {
                        incompatible("Receiver presence mismatch")
                    }
                    superMember.valueParameters.size != subMember.valueParameters.size -> {
                        incompatible("Value parameter number mismatch")
                    }
                    else -> null
                }
            }
            is IrProperty -> {
                require(subMember is IrProperty)
                if (superMember.getter?.extensionReceiverParameter == null != (subMember.getter?.extensionReceiverParameter == null)) {
                    incompatible("Receiver presence mismatch")
                } else null
            }
            else -> error("Unxpected declaration for value parameter check: $this")
        }
    }
}

fun IrSimpleFunction.isOverridableFunction(): Boolean =
    this.visibility != DescriptorVisibilities.PRIVATE &&
            this.dispatchReceiverParameter != null

fun IrProperty.isOverridableProperty(): Boolean =
    this.visibility != DescriptorVisibilities.PRIVATE &&
            (this.getter?.dispatchReceiverParameter != null ||
                    this.setter?.dispatchReceiverParameter != null)

fun IrDeclaration.isOverridableMemberOrAccessor(): Boolean = when (this) {
    is IrSimpleFunction -> isOverridableFunction()
    is IrProperty -> isOverridableProperty()
    else -> false
}
