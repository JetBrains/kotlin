/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.AbstractField
import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.config.AbstractBuilderConfigurator
import org.jetbrains.kotlin.generators.tree.isSubclassOf
import org.jetbrains.kotlin.generators.tree.hasLeafBuilder
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.imports.Importable
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Implementation
import org.jetbrains.kotlin.ir.generator.model.LeafBuilder

/**
 * Decides which IR elements get a generated builder, and what the default value of each of the builder's properties is.
 *
 * Only [Element.Category.Declaration] elements participate for now.
 *
 * Fields that end up without a default are printed as `lateinit` by [org.jetbrains.kotlin.generators.tree.AbstractBuilderPrinter],
 * which is the closest a builder gets to the compile-time requiredness that a constructor parameter has.
 */
class BuilderConfigurator(model: Model) : AbstractBuilderConfigurator<Element, Implementation, Field>(model) {

    override val namePrefix: String
        get() = "Ir"

    override val defaultBuilderPackage: String
        get() = "${Packages.declarations}.builder"

    override fun configureBuilders() {
        for (implementation in model.elements.flatMap { it.implementations }) {
            if (!implementation.isEligibleForBuilder) continue
            implementation.builder = LeafBuilder(implementation).also {
                // Name the builder after the element, not the implementation: `IrSimpleFunction`'s only implementation is called
                // `IrFunctionImpl`, and `IrFunctionImplBuilder`/`buildFunctionImpl` would leak that into the public API.
                it.nameOverride = implementation.element.typeName
            }
        }

        leafBuilders.groupBy { it.typeName }.forEach { [typeName, builders] ->
            require(builders.size == 1) {
                "${builders.size} implementations would generate $typeName: ${builders.joinToString { it.implementation.typeName }}"
            }
        }

        for (builder in leafBuilders) {
            for (field in builder.allFields) {
                // A field that the implementation initializes after construction already carries that default over.
                if (field.defaultValueInBuilder != null) continue
                builder.configureDefaultOf(field)
            }
            builder.importTypesUsedByCarriedOverDefaults()
        }
    }

    /**
     * A default that [Implementation.fieldsAssignedInBuilder] carried over is code written for the implementation's package, so it
     * may name something the builder's own package cannot see -- `IrScriptImpl`'s `SCRIPT_ORIGIN`, for instance. The
     * implementation declares those in `additionalImports`; take the ones this builder actually mentions.
     */
    private fun LeafBuilder.importTypesUsedByCarriedOverDefaults() {
        val defaults = allFields.mapNotNull { it.defaultValueInBuilder }
        usedTypes += implementation.additionalImports.filter { import ->
            defaults.any { Regex("\\b${Regex.escape(import.typeName)}\\b").containsMatchIn(it) }
        }
    }

    private val Implementation.isEligibleForBuilder: Boolean
        get() = element.category == Element.Category.Declaration && doPrint && kind?.hasLeafBuilder == true

    private val leafBuilders: List<LeafBuilder>
        get() = model.elements.flatMap { it.implementations }.mapNotNull { it.builder }

    private fun LeafBuilder.configureDefaultOf(field: Field) {
        when {
            // Every constructor is called `<init>`; `IrFactory.createConstructor` takes the name as a parameter only because
            // it takes every other property that way too. Without this the property would be `lateinit` and every call site
            // would have to spell it out.
            field.name == "name" && implementation.element.isSubclassOf(IrTree.constructor) ->
                setDefault(field, "SpecialNames.INIT", ArbitraryImportable("org.jetbrains.kotlin.name", "SpecialNames"))
            // A fresh symbol, as the hand-written builders hardcode -- but unlike them it stays settable, which is what lets the
            // deserializer and fir2ir paths, which bring their own symbols, use a builder at all.
            field.symbolFieldRole == AbstractField.SymbolFieldRole.DECLARED -> {
                val symbolImplName = (field.symbolClass?.typeName ?: return) + "Impl"
                setDefault(field, "$symbolImplName()", ArbitraryImportable(Packages.symbolsImpl, symbolImplName))
            }
            field.name == "startOffset" || field.name == "endOffset" ->
                setDefault(field, "UNDEFINED_OFFSET", ArbitraryImportable(Packages.tree, "UNDEFINED_OFFSET"))
            field.name == "origin" ->
                setDefault(field, "IrDeclarationOrigin.DEFINED", ArbitraryImportable(Packages.declarations, "IrDeclarationOrigin"))
            field.name == "visibility" ->
                setDefault(field, "DescriptorVisibilities.PUBLIC", ArbitraryImportable(Packages.descriptors, "DescriptorVisibilities"))
            field.name == "modality" ->
                setDefault(field, "Modality.FINAL", ArbitraryImportable(Packages.descriptors, "Modality"))
            field.name == "source" ->
                setDefault(field, "SourceElement.NO_SOURCE", ArbitraryImportable(Packages.descriptors, "SourceElement"))
            field.name == "variance" ->
                setDefault(field, "Variance.INVARIANT", ArbitraryImportable("org.jetbrains.kotlin.types", "Variance"))
            field.simpleTypeName == "ClassKind" ->
                setDefault(field, "ClassKind.CLASS", ArbitraryImportable(Packages.descriptors, "ClassKind"))
            field.simpleTypeName == "IrParameterKind" ->
                setDefault(field, "IrParameterKind.Regular", ArbitraryImportable(Packages.declarations, "IrParameterKind"))
            // `IrTypeParameter.index` is filled in from the container's size by `IrTypeParametersContainer.addTypeParameter`,
            // which a builder has no container to ask.
            field.name == "index" -> setDefault(field, "0")
            // Mirrors `IrFactory`, where every `Boolean` parameter that isn't inherently required defaults to `false`.
            field.typeRef == StandardTypes.boolean -> setDefault(field, "false")
        }
    }

    private fun LeafBuilder.setDefault(field: Field, value: String, vararg imports: Importable) {
        field.defaultValueInBuilder = value
        usedTypes += imports
    }

    private val Field.simpleTypeName: String?
        get() = (typeRef as? ClassRef<*>)?.simpleName
}
