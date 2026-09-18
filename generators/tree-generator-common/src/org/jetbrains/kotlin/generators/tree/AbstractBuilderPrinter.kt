/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.generators.tree.printer.printKDoc
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractBuilderPrinter<Element, Implementation, ElementField>(val printer: ImportCollectingPrinter)
        where Element : AbstractElement<Element, ElementField, Implementation>,
              Implementation : AbstractImplementation<Implementation, Element, ElementField>,
              ElementField : AbstractField<ElementField> {

    companion object {
        /**
         * The name of the parameter that an out-of-class `build` takes the builder as.
         */
        const val BUILDER_PARAMETER_NAME = "builder"

        private val experimentalContractsAnnotation =
            type("kotlin.contracts", "ExperimentalContracts", TypeKind.Class).toAnnotation()
    }

    protected abstract val implementationDetailAnnotation: PrintableAnnotation

    protected abstract val builderDslAnnotation: PrintableAnnotation

    protected open fun ImportCollectingPrinter.printFieldReferenceInImplementationConstructorCall(field: ElementField) {
        print(field.name)
    }

    protected open fun actualTypeOfField(field: ElementField): TypeRefWithNullability =
        if (field is ListField) StandardTypes.mutableList.withArgs(field.baseType) else field.typeRef

    /**
     * Which of the builder's fields are passed to the implementation's constructor.
     *
     * By default all of them, which is what a tree whose implementations take all of their fields in the constructor needs. A tree
     * that also initializes fields after construction narrows this and assigns the rest in [printBuildFunctionBody].
     */
    protected open fun fieldsInConstructorCall(builder: LeafBuilder<ElementField, Element, Implementation>): List<ElementField> =
        builder.allFields

    /**
     * Whether the generated `build` is `@PublishedApi internal` rather than public.
     *
     * A tree whose only intended entry point is the `buildX {}` DSL hides `build`, so that a caller cannot hand the same builder
     * to it twice and get two elements sharing a symbol. `@PublishedApi` rather than a plain `internal`, because the public
     * inline `buildX {}` is inlined into other modules and its bytecode names `build` directly.
     */
    protected open val buildFunctionIsPublishedApi: Boolean
        get() = false

    /**
     * The opt-in annotations that the generated `build()` needs.
     */
    protected open fun buildFunctionOptIns(builder: LeafBuilder<ElementField, Element, Implementation>): Set<PrintableAnnotation> =
        if (builder.implementation.isPublic) setOf(implementationDetailAnnotation) else emptySet()

    /**
     * Extra members to print in the builder class, after its properties.
     */
    protected open fun ImportCollectingPrinter.printAdditionalBuilderMethods(
        builder: LeafBuilder<ElementField, Element, Implementation>,
    ) {
    }

    /**
     * Documentation for the generated builder class, or `null` for none.
     */
    protected open fun builderKDoc(builder: LeafBuilder<ElementField, Element, Implementation>): String? = null

    /**
     * Documentation for the generated `build`, or `null` for none.
     */
    protected open fun buildFunctionKDoc(builder: LeafBuilder<ElementField, Element, Implementation>): String? = null

    /**
     * The type that owns the generated `build`, or `null` to keep it a member of the builder.
     *
     * When non-null, `build` is emitted as `fun <Receiver>.build(builder: XBuilder): X` next to the builder class instead of
     * inside it, and the fields in [fieldsSuppliedByBuildReceiver] disappear from the builder. Use it when building needs
     * something the builder itself should not carry -- something with no sensible default, that must come from the caller.
     */
    protected open fun buildFunctionReceiver(
        builder: LeafBuilder<ElementField, Element, Implementation>,
    ): TypeRef? = null

    /**
     * Fields that [buildFunctionReceiver] supplies, and which the builder therefore does not declare.
     */
    protected open fun fieldsSuppliedByBuildReceiver(
        builder: LeafBuilder<ElementField, Element, Implementation>,
    ): List<ElementField> = emptyList()

    /**
     * The type that the generated `buildX {}` function is an extension of, or `null` for a top-level function.
     */
    protected open fun dslBuildFunctionExtensionReceiver(
        builder: LeafBuilder<ElementField, Element, Implementation>,
    ): TypeRef? = null

    /**
     * How the generated `buildX {}` function instantiates the builder, before `init` is applied to it. A tree whose builders need
     * something from [dslBuildFunctionExtensionReceiver] hands it over here.
     */
    protected open fun ImportCollectingPrinter.printBuilderInstantiation(
        builder: LeafBuilder<ElementField, Element, Implementation>,
    ) {
        print(builder.render(), "()")
    }

    /**
     * The body of the generated `build()`.
     */
    protected open fun ImportCollectingPrinter.printBuildFunctionBody(
        builder: LeafBuilder<ElementField, Element, Implementation>,
    ) {
        println("return ${builder.implementation.render()}(")
        withIndent {
            for (field in fieldsInConstructorCall(builder)) {
                if (field.invisibleField) continue
                printFieldReferenceInImplementationConstructorCall(field)
                println(",")
            }
        }
        println(")")
    }

    protected open fun copyField(field: ElementField, originalParameterName: String, copyBuilderVariableName: String) {
        printer.run {
            when {
                field is ListField -> println(
                    copyBuilderVariableName,
                    ".",
                    field.name,
                    ".addAll(",
                    originalParameterName,
                    ".",
                    field.name,
                    ")",
                )
                else -> println(copyBuilderVariableName, ".", field.name, " = ", originalParameterName, ".", field.name)
            }
        }
    }

    fun printBuilder(builder: Builder<ElementField, Element>) {
        printer.run {
            addAllImports(builder.usedTypes)
            if (builder is LeafBuilder<*, *, *> && builder.allFields.isEmpty()) {
                @Suppress("UNCHECKED_CAST")
                printDslBuildFunction(builder as LeafBuilder<ElementField, Element, Implementation>, hasRequiredFields = false)
                return
            }

            @Suppress("UNCHECKED_CAST")
            (builder as? LeafBuilder<ElementField, Element, Implementation>)?.let { printKDoc(builderKDoc(it)) }
            println(builderDslAnnotation.render())
            when (builder) {
                is IntermediateBuilder -> print("${if (builder.isSealed) "sealed " else ""}interface ")
                is LeafBuilder<*, *, *> -> {
                    if (builder.isOpen) {
                        print("open ")
                    }
                    print("class ")
                }
            }
            print(builder.render())
            @Suppress("UNCHECKED_CAST")
            val leafBuilder = builder as? LeafBuilder<ElementField, Element, Implementation>
            val buildReceiver = leafBuilder?.let { buildFunctionReceiver(it) }
            val fieldsFromReceiver = leafBuilder?.let { fieldsSuppliedByBuildReceiver(it) }.orEmpty()
            if (builder.parents.isNotEmpty()) {
                print(builder.parents.joinToString(separator = ", ", prefix = " : ") { it.render() })
            }
            var hasRequiredFields = false
            printBlock {
                var needNewLine = false
                for (field in builder.allFields) {
                    if (field in fieldsFromReceiver) continue
                    val [newLine, requiredFields] = printFieldInBuilder(field, builder, fieldIsUseless = false)
                    needNewLine = newLine
                    hasRequiredFields = hasRequiredFields || requiredFields
                }
                val hasBackingFields = builder.allFields.any { it.nullable }
                if (needNewLine && buildReceiver == null) {
                    println()
                }
                val buildType = when (builder) {
                    is LeafBuilder<*, *, *> -> builder.implementation.element.render()
                    is IntermediateBuilder -> builder.materializedElement!!.withStarArgs().render()
                }
                if (buildReceiver == null) {
                    if (leafBuilder != null) {
                        val optIns = buildFunctionOptIns(leafBuilder)
                        if (optIns.isNotEmpty()) {
                            println("@OptIn(", optIns.joinToString { it.asClassRefString }, ")")
                        }
                    }
                    if (leafBuilder != null && buildFunctionIsPublishedApi) {
                        println("@PublishedApi")
                    }
                    if (builder.parents.isNotEmpty()) {
                        print("override ")
                    }
                    if (leafBuilder != null && buildFunctionIsPublishedApi) {
                        print("internal ")
                    }
                    print("fun build(): ", buildType)
                    if (leafBuilder != null) {
                        printBlock { printBuildFunctionBody(leafBuilder) }
                        if (hasBackingFields) {
                            println()
                        }
                    } else {
                        println()
                    }
                }

                if (leafBuilder != null) {
                    printAdditionalBuilderMethods(leafBuilder)
                }
                if (builder is LeafBuilder<*, *, *>) {
                    if (builder.uselessFields.isNotEmpty()) {
                        println()
                        builder.uselessFields.forEachIndexed { index, field ->
                            if (index > 0) {
                                println()
                            }
                            printFieldInBuilder(field, builder, fieldIsUseless = true)
                        }
                    }
                }
            }
            if (leafBuilder != null && buildReceiver != null) {
                println()
                printKDoc(buildFunctionKDoc(leafBuilder))
                val optIns = buildFunctionOptIns(leafBuilder)
                if (optIns.isNotEmpty()) {
                    println("@OptIn(", optIns.joinToString { it.asClassRefString }, ")")
                }
                if (buildFunctionIsPublishedApi) {
                    println("@PublishedApi")
                }
                printFunctionWithBlockBody(
                    name = "build",
                    parameters = listOf(FunctionParameter(name = BUILDER_PARAMETER_NAME, type = leafBuilder)),
                    returnType = leafBuilder.implementation.element,
                    typeParameters = leafBuilder.implementation.element.params,
                    extensionReceiver = buildReceiver,
                    visibility = if (buildFunctionIsPublishedApi) Visibility.INTERNAL else Visibility.PUBLIC,
                ) {
                    println("with(", BUILDER_PARAMETER_NAME, ") {")
                    withIndent { printBuildFunctionBody(leafBuilder) }
                    println("}")
                }
            }
            if (builder is LeafBuilder<*, *, *>) {
                println()
                @Suppress("UNCHECKED_CAST")
                printDslBuildFunction(builder as LeafBuilder<ElementField, Element, Implementation>, hasRequiredFields)

                if (builder.wantsCopy) {
                    println()
                    printDslBuildCopyFunction(builder, hasRequiredFields)
                }
            }
        }
    }

    private fun lambdaParameterForBuilderFunction(builder: Builder<ElementField, Element>, hasRequiredFields: Boolean) =
        FunctionParameter(
            name = "init",
            type = Lambda(receiver = builder, returnType = StandardTypes.unit),
            defaultValue = "{}".takeIf { !hasRequiredFields },
        )

    private fun ImportCollectingPrinter.contractCallsInPlaceExactlyOnce() {
        addStarImport("kotlin.contracts")
        print("contract")
        printBlock {
            println("callsInPlace(init, InvocationKind.EXACTLY_ONCE)")
        }
    }

    private fun builderFunctionName(builder: LeafBuilder<ElementField, Element, Implementation>) =
        "build" + builder.builderBaseName.removePrefix(builder.implementation.namePrefix)

    private fun ImportCollectingPrinter.printDslBuildFunction(
        builder: LeafBuilder<ElementField, Element, Implementation>,
        hasRequiredFields: Boolean,
    ) {
        val isEmpty = builder.allFields.isEmpty()
        if (!isEmpty) {
            println("@OptIn(", experimentalContractsAnnotation.asClassRefString, ")")
        } else if (builder.implementation.isPublic) {
            println("@OptIn(", implementationDetailAnnotation.asClassRefString, ")")
        }
        val initParameter = if (isEmpty) null else lambdaParameterForBuilderFunction(builder, hasRequiredFields)
        printFunctionWithBlockBody(
            name = builderFunctionName(builder),
            parameters = listOfNotNull(initParameter),
            returnType = builder.implementation.element,
            typeParameters = builder.implementation.element.params,
            extensionReceiver = dslBuildFunctionExtensionReceiver(builder),
            isInline = !isEmpty,
        ) {
            if (!isEmpty) {
                addStarImport("kotlin.contracts")
                println("contract {")
                withIndent {
                    println("callsInPlace(init, InvocationKind.EXACTLY_ONCE)")
                }
                println("}")
            }
            print("return ")
            when {
                isEmpty -> println(builder.implementation.render(), "()")
                // `build` lives on the receiver, so the builder is passed to it rather than asked to build itself.
                buildFunctionReceiver(builder) != null -> {
                    print("build(")
                    printBuilderInstantiation(builder)
                    println(".apply(init))")
                }
                else -> {
                    printBuilderInstantiation(builder)
                    println(".apply(init).build()")
                }
            }
        }
    }

    private fun ElementField.needBackingField(fieldIsUseless: Boolean) =
        !nullable && this !is ListField && if (fieldIsUseless) {
            implementationDefaultStrategy?.defaultValue == null
        } else {
            defaultValueInBuilder == null
        }

    private fun ElementField.needNotNullDelegate(fieldIsUseless: Boolean) =
        needBackingField(fieldIsUseless) && (typeRef == StandardTypes.boolean || typeRef == StandardTypes.int)

    private fun ImportCollectingPrinter.printFieldInBuilder(
        field: ElementField,
        builder: Builder<ElementField, Element>,
        fieldIsUseless: Boolean,
    ): Pair<Boolean, Boolean> {
        if (
            field.implementationDefaultStrategy?.withGetter == true
            && !fieldIsUseless || field.invisibleField
        ) return false to false
        if (field is ListField) {
            @Suppress("UNCHECKED_CAST")
            printFieldListInBuilder(field as ElementField, builder, fieldIsUseless)
            return true to false
        }
        val defaultValue = if (fieldIsUseless)
            field.implementationDefaultStrategy!!.defaultValue
        else
            field.defaultValueInBuilder

        printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
        printModifiers(builder, field, fieldIsUseless)
        print("var ${field.name}: ${field.typeRef.render()}")
        var hasRequiredFields = false
        val needNewLine = when {
            fieldIsUseless -> {
                println()
                withIndent {
                    println("get() = throw IllegalStateException()")
                    println("set(_) {")
                    withIndent {
                        println("throw IllegalStateException()")
                    }
                    println("}")
                }
                true
            }
            builder is IntermediateBuilder -> {
                println()
                false
            }
            field.needNotNullDelegate(fieldIsUseless = false) -> {
                println(" by kotlin.properties.Delegates.notNull<${field.typeRef.render()}>()")
                hasRequiredFields = true
                true
            }
            field.needBackingField(fieldIsUseless = false) -> {
                println()
                hasRequiredFields = true
                true
            }
            else -> {
                println(" = $defaultValue")
                true
            }
        }
        return needNewLine to hasRequiredFields
    }

    private fun ImportCollectingPrinter.printDeprecationOnUselessFieldIfNeeded(
        field: AbstractField<*>,
        builder: Builder<ElementField, Element>,
        fieldIsUseless: Boolean,
    ) {
        if (fieldIsUseless) {
            println(
                "@Deprecated(\"Modification of '",
                field.name,
                "' has no impact for ",
                builder.typeName,
                "\", level = DeprecationLevel.HIDDEN)",
            )
        }
    }

    private fun ImportCollectingPrinter.printFieldListInBuilder(
        field: ElementField,
        builder: Builder<ElementField, Element>,
        fieldIsUseless: Boolean,
    ) {
        printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
        printModifiers(builder, field, fieldIsUseless)
        print("val ", field.name, ": ", actualTypeOfField(field).render())
        if (builder is LeafBuilder<*, *, *>) {
            print(" = []")
        }
        println()
    }

    private fun ImportCollectingPrinter.printModifiers(builder: Builder<ElementField, Element>, field: AbstractField<*>, fieldIsUseless: Boolean) {
        if (builder is IntermediateBuilder) {
            print("abstract ")
        }
        if (builder.isFromParent(field)) {
            print("override ")
        } else if (builder is LeafBuilder<*, *, *> && builder.isOpen) {
            print("open ")
        }
        @Suppress("UNCHECKED_CAST")
        if (builder is LeafBuilder<*, *, *> &&
            (field as ElementField).needBackingField(fieldIsUseless) &&
            !fieldIsUseless &&
            !field.needNotNullDelegate(fieldIsUseless = false)
        ) {
            print("lateinit ")
        }
    }

    private fun ImportCollectingPrinter.printDslBuildCopyFunction(
        builder: LeafBuilder<ElementField, Element, Implementation>,
        hasRequiredFields: Boolean,
    ) {
        val optIns = builder.allFields
            .filter { !it.invisibleField }
            .mapNotNullTo(mutableSetOf(experimentalContractsAnnotation)) { it.optInAnnotation }
        println("@OptIn(", optIns.joinToString { it.asClassRefString }, ")")
        val originalParameter = FunctionParameter(name = "original", type = builder.implementation.element)
        val initParameter = lambdaParameterForBuilderFunction(builder, hasRequiredFields)
        printFunctionWithBlockBody(
            name = builderFunctionName(builder) + "Copy",
            parameters = listOf(originalParameter, initParameter),
            returnType = builder.implementation.element,
            typeParameters = builder.implementation.element.params,
            isInline = true,
        ) {
            print("contract")
            printBlock {
                println("callsInPlace(init, InvocationKind.EXACTLY_ONCE)")
            }
            val copyBuilderVariableName = "copyBuilder"
            println("val ", copyBuilderVariableName, " = ", builder.render(), "()")
            for (field in builder.allFields) {
                if (field.invisibleField || field.skippedInCopy) continue
                copyField(field, originalParameter.name, copyBuilderVariableName)
            }
            println("return ", copyBuilderVariableName, ".apply(", initParameter.name, ").build()")
        }
    }
}
