/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.AbstractBuilderPrinter
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.isSubclassOf
import org.jetbrains.kotlin.generators.tree.LeafBuilder
import org.jetbrains.kotlin.generators.tree.PrintableAnnotation
import org.jetbrains.kotlin.generators.tree.TypeRef
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.printKDoc
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.irBuilderDslAnnotation
import org.jetbrains.kotlin.ir.generator.irFactoryType
import org.jetbrains.kotlin.ir.generator.irImplementationDetailType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.MapField
import org.jetbrains.kotlin.ir.generator.model.Implementation
import org.jetbrains.kotlin.utils.withIndent

/**
 * Generates the builders of IR declarations, for example `IrClassBuilder` and `IrFactory.buildClass {}`.
 *
 * The generated `build()` reproduces what the hand-written `IrFactory.create*` methods do: construct the implementation, let the
 * factory observe the new declaration, and only then initialize the properties that the constructor doesn't accept. Going through
 * the factory is not optional -- `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` hook `declarationCreated()` to attach
 * `IdSignature`s, and a builder that bypassed it would silently break incremental compilation.
 */
internal class BuilderPrinter(printer: ImportCollectingPrinter) : AbstractBuilderPrinter<Element, Implementation, Field>(printer) {

    /**
     * `setSourceRange` and `updateFrom`, which the hand-written builders offered from a shared base class.
     *
     * The generated builders have no common supertype, so both are emitted per builder instead. That is a little repetitive but
     * needs no intermediate builder interface, and `updateFrom` has to be per builder anyway: its parameter is the element type.
     */
    override fun ImportCollectingPrinter.printAdditionalBuilderMethods(builder: LeafBuilder<Field, Element, Implementation>) {
        val element = builder.implementation.element
        val hasOffsets = builder.allFields.any { it.name == "startOffset" }
        if (hasOffsets) {
            println()
            printKDoc("Takes the source range of [from], the way `IrElement.startOffset` and `endOffset` are usually copied.")
            printFunctionWithBlockBody(
                name = "setSourceRange",
                parameters = listOf(FunctionParameter(name = "from", type = IrTree.rootElement)),
                returnType = StandardTypes.unit,
            ) {
                println("startOffset = from.startOffset")
                println("endOffset = from.endOffset")
            }
        }

        val suppliedByReceiver = fieldsSuppliedByBuildReceiver(builder)
        val copied = builder.allFields.filter { it !in suppliedByReceiver && it.isCopiedByUpdateFrom(element) }
        if (copied.isNotEmpty()) {
            println()
            printKDoc(
                "Copies the properties of [from] that identify a *kind* of ${element.typeName}, not a particular one,\n" +
                        "including its source range.\n" +
                        "\n" +
                        "Its symbol, parent and children are left alone: sharing a symbol between two declarations would bind both\n" +
                        "to one, and sharing a body or a receiver would splice one tree into another. Properties the element\n" +
                        "declares `lateinit` are skipped too, since reading one that was never set throws."
            )
            printFunctionWithBlockBody(
                name = "updateFrom",
                parameters = listOf(FunctionParameter(name = "from", type = element)),
                returnType = StandardTypes.unit,
            ) {
                for (field in copied) {
                    println(field.name, " = from.", field.name)
                }
            }
        }
    }

    /**
     * Whether `updateFrom` copies this field off an existing element of type [element].
     */
    private fun Field.isCopiedByUpdateFrom(element: Element): Boolean =
        when {
            // `metadata` is the descriptor the backend writes into the class file's Kotlin metadata, so it describes one
            // particular declaration rather than a kind of one. The hand-written builders drew the line by element: only
            // `IrFieldBuilder` had a `metadata` property and copied it, which `JvmCachedDeclarations.getStaticBackingField`
            // relies on to move a companion field's metadata along with the field. Copying it for a function or a property
            // instead makes a default-argument or inline-class replacement claim a signature that is not its own, and
            // reflection over it then fails.
            name == "metadata" -> element.isSubclassOf(IrTree.field)
            // A declared symbol would bind two declarations to one; a referenced symbol is a graph edge, so it belongs to the
            // element's position in the tree rather than to its character.
            symbolFieldRole != null -> false
            // The element declares it `lateinit`, so reading it off a half-built element throws.
            assignedInBuilderIfNotNull -> false
            containsElement -> false
            // Copying a list would alias it; `IrTypeParameter.superTypes` in particular usually wants remapping instead.
            this is ListField || this is MapField -> false
            else -> true
        }

    override fun builderKDoc(builder: LeafBuilder<Field, Element, Implementation>): String? = buildString {
        appendLine("Collects the properties of a [${builder.implementation.element.typeName}] and builds one.")
        appendLine()
        appendLine("A property with no sensible default is declared `lateinit`, so building without it throws rather than")
        appendLine("inventing a value. There is no way to express \"required\" for a property the caller assigns inside a")
        appendLine("lambda; a constructor parameter could, at the cost of the property being settable only once.")
        if (builder.implementation.goesThroughFactory) {
            appendLine()
            append("Built by [${irFactoryType.simpleName}.build], which is where the factory comes from.")
        }
    }.trimEnd()

    override fun buildFunctionKDoc(builder: LeafBuilder<Field, Element, Implementation>): String =
        "Builds the collected [${builder.implementation.element.typeName}].\n" +
                "\n" +
                "`declarationCreated` is not decoration: `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` override it to\n" +
                "attach an `IdSignature` to every declaration they create. Skipping it would leave declarations unsigned, and\n" +
                "incremental compilation would cache the wrong thing without failing."

    /**
     * `IrFactory.build(builder)` is not an entry point: handing one builder to it twice would produce two declarations sharing a
     * symbol and a mutable `overriddenSymbols` list. `buildX {}` is the only way in, and it allocates the builder itself.
     */
    override val buildFunctionIsPublishedApi: Boolean
        get() = true

    override val implementationDetailAnnotation: PrintableAnnotation
        get() = irImplementationDetailType

    override val builderDslAnnotation: PrintableAnnotation
        get() = irBuilderDslAnnotation

    override fun fieldsInConstructorCall(builder: LeafBuilder<Field, Element, Implementation>): List<Field> =
        builder.implementation.fieldsInConstructor

    override fun buildFunctionOptIns(builder: LeafBuilder<Field, Element, Implementation>): Set<PrintableAnnotation> =
        buildSet {
            // Mirrors the condition under which `AbstractImplementationPrinter` puts `@IrImplementationDetail` on the constructor.
            // Implementations whose constructor is not public, such as `IrVariableImpl`, don't have it and must not be opted into:
            // an unnecessary `@OptIn` is a warning, and the compiler build treats warnings as errors.
            if (builder.implementation.run { isPublic && isConstructorPublic && putImplementationOptInInConstructor }) {
                add(implementationDetailAnnotation)
            }
            builder.allFields.mapNotNullTo(this) { it.optInAnnotation }
        }

    override fun ImportCollectingPrinter.printFieldReferenceInImplementationConstructorCall(field: Field) {
        // Named arguments, so that the generated code is insensitive to `constructorParameterOrderOverride`.
        print(field.name, " = ", field.name)
    }

    /**
     * A declaration is built *by* a factory, so `build` belongs to the factory rather than to the builder.
     *
     * The factory has no sensible default and must not be guessed: `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` hook
     * `declarationCreated()` to attach an `IdSignature`, so a builder that quietly fell back to `IrFactoryImpl` would hand back
     * declarations with no signature and no error. Taking it from the receiver makes supplying it unavoidable, and keeps it out
     * of the builder's properties, where it could shadow a `factory` in scope at the call site.
     *
     * Declarations that store no factory -- `IrVariable`, `IrFile` and the package fragments -- keep `build()` as a member of
     * their builder.
     */
    override fun buildFunctionReceiver(builder: LeafBuilder<Field, Element, Implementation>): TypeRef? =
        irFactoryType.takeIf { builder.implementation.goesThroughFactory }

    override fun fieldsSuppliedByBuildReceiver(builder: LeafBuilder<Field, Element, Implementation>): List<Field> =
        builder.allFields.filter { it.name == FACTORY_FIELD_NAME }

    override fun dslBuildFunctionExtensionReceiver(builder: LeafBuilder<Field, Element, Implementation>): TypeRef? =
        irFactoryType.takeIf { builder.implementation.goesThroughFactory }

    override fun ImportCollectingPrinter.printBuildFunctionBody(builder: LeafBuilder<Field, Element, Implementation>) {
        val implementation = builder.implementation

        print("val ", RESULT_VARIABLE_NAME, " = ")
        printConstructorCall(builder)

        for (field in implementation.fieldsAssignedInBuilder) {
            when {
                field.assignedInBuilderIfNotNull ->
                    // `IrFactory` leaves a `lateinit` property uninitialized when it wasn't given a value, so does the builder.
                    println(field.name, "?.let { ", RESULT_VARIABLE_NAME, ".", field.name, " = it }")
                else -> println(RESULT_VARIABLE_NAME, ".", field.name, " = ", field.name)
            }
        }

        println("return ", RESULT_VARIABLE_NAME)
    }

    private fun ImportCollectingPrinter.printConstructorCall(builder: LeafBuilder<Field, Element, Implementation>) {
        val implementation = builder.implementation
        println(implementation.render(), "(")
        withIndent {
            // `IrVariableImpl` takes an extra unused parameter whose only purpose is to keep its constructor out of reach of
            // outside code, see `IrElementConstructorIndicator`.
            if (implementation.hasConstructorIndicator) {
                println("null,")
            }
            for (field in fieldsInConstructorCall(builder)) {
                if (field.invisibleField) continue
                if (field.name == FACTORY_FIELD_NAME) {
                    // The factory is the receiver of the generated `build`.
                    println(FACTORY_FIELD_NAME, " = this@build,")
                    continue
                }
                printFieldReferenceInImplementationConstructorCall(field)
                println(",")
            }
        }
        print(")")
        if (implementation.goesThroughFactory) {
            print(".declarationCreated()")
        }
        println()
    }

    /**
     * Whether the implementation stores the [org.jetbrains.kotlin.ir.declarations.IrFactory] that created it, and therefore has to
     * be created through that factory. `IrVariableImpl` is the one declaration implementation that doesn't.
     */
    private val Implementation.goesThroughFactory: Boolean
        get() = fieldsInConstructor.any { it.name == FACTORY_FIELD_NAME }

    private companion object {

        /**
         * No `IrDeclaration` has a field of this name, so the local variable cannot shadow one of the builder's properties.
         */
        const val RESULT_VARIABLE_NAME = "result"

        const val FACTORY_FIELD_NAME = "factory"
    }
}
