/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
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
     * Creates a fake override for [member] from [superType] to be added to the class [clazz]
     */
    fun fakeOverrideMember(superType: IrType, member: IrOverridableMember, clazz: IrClass): IrOverridableMember =
        buildFakeOverrideMember(superType, member, clazz, friendModules, unimplementedOverridesStrategy)

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

    /**
     * Most implementations need [file] in which they are working now.
     *
     * It should be avoided in the future, but for now it's like this.
     * For now, it's called with class file when class processing is started.
     *
     * Contract:
     *  * must call [block] exactly once.
     */
    abstract fun inFile(file: IrFile?, block: () -> Unit)

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
}

@OptIn(ObsoleteDescriptorBasedAPI::class) // Because of the LazyIR, have to use descriptors here.
private fun IrOverridableMember.isPrivateToThisModule(
    thisClass: IrClass, memberClass: IrClass, friendModules: Map<String, Collection<String>>,
): Boolean {
    if (visibility != DescriptorVisibilities.INTERNAL) return false

    val thisModule = thisClass.getPackageFragment().packageFragmentDescriptor.containingDeclaration
    val memberModule = memberClass.getPackageFragment().packageFragmentDescriptor.containingDeclaration

    return thisModule != memberModule && !isInFriendModules(thisModule, memberModule, friendModules)
}

private fun isInFriendModules(
    fromModule: ModuleDescriptor,
    toModule: ModuleDescriptor,
    friendModules: Map<String, Collection<String>>,
): Boolean {
    if (friendModules.isEmpty()) return false

    val fromModuleName = fromModule.name.asStringStripSpecialMarkers()

    val fromFriends = friendModules[fromModuleName] ?: return false

    val toModuleName = toModule.name.asStringStripSpecialMarkers()

    return toModuleName in fromFriends
}

fun buildFakeOverrideMember(
    superType: IrType,
    member: IrOverridableMember,
    clazz: IrClass,
    friendModules: Map<String, Collection<String>> = emptyMap(),
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
        .apply {
            makeExternal(clazz.isExternal)

            val isInvisible = isPrivateToThisModule(clazz, classifier.owner, friendModules)
            if (isInvisible && !member.annotations.hasAnnotation(StandardNames.FqNames.publishedApi))
                visibility = DescriptorVisibilities.INVISIBLE_FAKE
        }
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
