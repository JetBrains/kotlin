/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

/**
 * This class is a customization point for IrFakeOverrideBuilder.
 *
 * While the main [IrFakeOverrideBuilder] class works on:
 *   * Choosing for which functions fake overrides should be created.
 *   * Handling merging overrides coming from different super types
 *
 * These class inheritors are responsible for:
 *   * Actually creating the fake override for a single member of a single super type
 *   * Creating and registering in appropriate storages of fake override member symbols
 *
 */
abstract class FakeOverrideBuilderStrategy(
    private val friendModules: Map<String, Collection<String>>,
    private val unimplementedOverridesStrategy: IrUnimplementedOverridesStrategy
) {

    /**
     * This flag enables workaround for KT-65504 and KT-42020.
     *
     * If there are several fake overrides in generic class, which becomes equivalent after
     * generic parameter substitution in subtype, several separate equivalent fake overrides were generated
     * because of incorrect optimization. This behavior existed for a long time, and would require some
     * additional effort to deprecate, so we keep the ability to keep it where reasonable.
     */
    open val isGenericClashFromSameSupertypeAllowed: Boolean = false

    /**
     * True iff it's not allowed to override an internal `@PublishedApi` function with an internal function from another module.
     *
     * On JVM, internal functions in classes are mangled unless they're annotated with `@PublishedApi`. This leads to a bug KT-61132 where
     * an internal `@PublishedApi` function can be accidentally overridden by a public function in another module. This bug should be fixed,
     * but until it is, we're replicating this behavior in klib-based backends, basically by treating internal `@PublishedApi` functions as
     * public. This flag controls this behavior. Note that it is enabled only on JVM, which unfortunately means that KT-67114 is still
     * a problem on klib-based backends.
     */
    open val isOverrideOfPublishedApiFromOtherModuleDisallowed: Boolean = false

    /**
     * Creates a fake override for [member] from [superType] to be added to the class [clazz] or returns null,
     * if no fake override should be created for this member
     */
    fun fakeOverrideMember(superType: IrType, member: IrOverridableMember, clazz: IrClass): IrOverridableMember? {
        return if (isVisibleForOverrideInClass(member, clazz))
            buildFakeOverrideMember(superType, member, clazz, unimplementedOverridesStrategy)
        else
            null
    }

    /**
     * This function is a callback for fake override creation finish.
     *
     * It can modify the created fake override, if needed.
     */
    fun postProcessGeneratedFakeOverride(fakeOverride: IrOverridableMember, clazz: IrClass) {
        unimplementedOverridesStrategy.postProcessGeneratedFakeOverride(fakeOverride as IrOverridableDeclaration<*>, clazz)
    }

    /**
     * Create a symbol for the fake override.
     */
    fun linkFakeOverride(fakeOverride: IrOverridableMember, compatibilityMode: Boolean) {
        when (fakeOverride) {
            is IrFunctionWithLateBinding -> linkFunctionFakeOverride(fakeOverride, compatibilityMode)
            is IrPropertyWithLateBinding -> linkPropertyFakeOverride(fakeOverride, compatibilityMode)
            else -> error("Unexpected fake override: $fakeOverride")
        }
    }

    private fun isInFriendModules(
        fromModule: ModuleDescriptor,
        toModule: ModuleDescriptor,
    ): Boolean {
        val fromModuleName = fromModule.name.asStringStripSpecialMarkers()
        val toModuleName = toModule.name.asStringStripSpecialMarkers()

        return fromModuleName == toModuleName || friendModules[fromModuleName]?.contains(toModuleName) == true
    }

    private fun isVisibleForOverrideInClass(original: IrOverridableMember, clazz: IrClass) : Boolean {
        return when {
            DescriptorVisibilities.isPrivate(original.visibility) -> false
            original.visibility == DescriptorVisibilities.INVISIBLE_FAKE -> false
            original.visibility == DescriptorVisibilities.INTERNAL -> {
                val thisModule = clazz.getPackageFragment().moduleDescriptor
                val memberModule = original.getPackageFragment().moduleDescriptor

                when {
                    thisModule == memberModule -> true
                    isInFriendModules(thisModule, memberModule) -> true
                    !isOverrideOfPublishedApiFromOtherModuleDisallowed &&
                            original.hasAnnotation(StandardClassIds.Annotations.PublishedApi) -> true
                    else -> false
                }
            }
            else -> {
                original.visibility.visibleFromPackage(
                    clazz.getPackageFragment().packageFqName,
                    original.getPackageFragment().packageFqName
                )
            }
        }
    }

    /**
     * Most implementations need [file] in which they are working now.
     *
     * It should be avoided in the future, but for now it's like this.
     * For now, it's called with class file when class processing is started.
     *
     * Contract:
     *  * must call [block] exactly once.
     */
    abstract fun <R> inFile(file: IrFile?, block: () -> R): R

    /**
     * Callback for creating a symbol for fake override function.
     *
     * Contract:
     *   * [IrFunctionWithLateBinding.acquireSymbol] must be called inside on [function] argument
     */
    protected abstract fun linkFunctionFakeOverride(function: IrFunctionWithLateBinding, manglerCompatibleMode: Boolean)

    /**
     * Callback for creating a symbol for fake override property.
     *
     * Also, must create symbols for property's getter and setter.
     *
     * Contract:
     *   * [IrPropertyWithLateBinding.acquireSymbol] must be called inside on [property] argument
     *   * [IrFunctionWithLateBinding.acquireSymbol] must be called inside on getter and setter of [property] argument, if they exist
     */
    protected abstract fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, manglerCompatibleMode: Boolean)

    abstract class BindToPrivateSymbols(friendModules: Map<String, Collection<String>>) : FakeOverrideBuilderStrategy(
        friendModules = friendModules,
        unimplementedOverridesStrategy = IrUnimplementedOverridesStrategy.ProcessAsFakeOverrides
    ) {
        override fun linkFunctionFakeOverride(function: IrFunctionWithLateBinding, manglerCompatibleMode: Boolean) {
            function.acquireSymbol(IrSimpleFunctionSymbolImpl())
        }

        override fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, manglerCompatibleMode: Boolean) {
            property.acquireSymbol(IrPropertySymbolImpl())

            property.getter?.let {
                it.correspondingPropertySymbol = property.symbol
                linkFunctionFakeOverride(it as? IrFunctionWithLateBinding ?: error("Unexpected fake override getter: $it"), manglerCompatibleMode)
            }
            property.setter?.let {
                it.correspondingPropertySymbol = property.symbol
                linkFunctionFakeOverride(it as? IrFunctionWithLateBinding ?: error("Unexpected fake override setter: $it"), manglerCompatibleMode)
            }
            property.backingField?.let {
                it.correspondingPropertySymbol = property.symbol
            }
        }

        override fun <R> inFile(file: IrFile?, block: () -> R): R = block()
    }
}

fun buildFakeOverrideMember(
    superType: IrType,
    member: IrOverridableMember,
    clazz: IrClass,
    unimplementedOverridesStrategy: IrUnimplementedOverridesStrategy = IrUnimplementedOverridesStrategy.ProcessAsFakeOverrides,
): IrOverridableMember {
    require(superType is IrSimpleType) { "superType is $superType, expected IrSimpleType" }
    val classifier = superType.classifier
    require(classifier is IrClassSymbol) { "superType classifier is not IrClassSymbol: $classifier" }

    val typeParameters = extractTypeParameters(classifier.owner)
    val superArguments = superType.arguments
    require(typeParameters.size == superArguments.size) {
        "typeParameters = $typeParameters size != typeArguments = $superArguments size "
    }

    val substitutionMap = mutableMapOf<IrTypeParameterSymbol, IrType>()

    for (i in typeParameters.indices) {
        val tp = typeParameters[i]
        val ta = superArguments[i]
        require(ta is IrTypeProjection) { "Unexpected super type argument: ${ta.render()} @ $i" }
        require(ta.variance == Variance.INVARIANT) { "Unexpected variance in super type argument: ${ta.variance} @$i" }
        substitutionMap[tp.symbol] = ta.type
    }

    return CopyIrTreeWithSymbolsForFakeOverrides(member, substitutionMap, clazz, unimplementedOverridesStrategy)
        .copy()
        .apply { makeExternal(clazz.isExternal) }
}

// TODO: this is JS-specific functionality which should be moved out of the common ir.tree.
private fun IrOverridableMember.makeExternal(value: Boolean) {
    when (this) {
        is IrSimpleFunction -> isExternal = value
        is IrProperty -> {
            isExternal = value
            getter?.isExternal = value
            setter?.isExternal = value
        }
    }
}
