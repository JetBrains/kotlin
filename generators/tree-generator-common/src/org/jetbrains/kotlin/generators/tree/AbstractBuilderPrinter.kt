/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.imports.ImportCollector
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractBuilderPrinter<Element, Implementation, BuilderField, ElementField>(val printer: ImportCollectingPrinter)
        where Element : AbstractElement<Element, ElementField, Implementation>,
              Implementation : AbstractImplementation<Implementation, Element, BuilderField>,
              BuilderField : AbstractField<*>,
              ElementField : AbstractField<ElementField> {

    companion object {
        private val experimentalContractsAnnotation =
            type("kotlin.contracts", "ExperimentalContracts", TypeKind.Class)
    }

    protected abstract val implementationDetailAnnotation: ClassRef<*>

    protected abstract val builderDslAnnotation: ClassRef<*>

    protected open fun ImportCollectingPrinter.printFieldReferenceInImplementationConstructorCall(field: BuilderField) {
        print(field.name)
    }

    protected open fun actualTypeOfField(field: ElementField): TypeRefWithNullability =
        if (field is ListField) StandardTypes.mutableList.withArgs(field.baseType) else field.typeRef

    protected open fun copyField(field: BuilderField, originalParameterName: String, copyBuilderVariableName: String) {
        printer.run {
            when {
                field.origin is ListField -> println(
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

    fun printBuilder(builder: Builder<BuilderField, Element>) {
        printer.run {
            addAllImports(builder.usedTypes)
            if (builder is LeafBuilder<*, *, *> && builder.allFields.isEmpty()) {
                @Suppress("UNCHECKED_CAST")
                printDslBuildFunction(builder as LeafBuilder<BuilderField, Element, Implementation>, hasRequiredFields = false)
                return
            }

            println("@", builderDslAnnotation.render())
            when (builder) {
                is IntermediateBuilder -> print("interface ")
                is LeafBuilder<*, *, *> -> {
                    if (builder.isOpen) {
                        print("open ")
                    }
                    print("class ")
                }
            }
            print(builder.render())
            if (builder.parents.isNotEmpty()) {
                print(builder.parents.joinToString(separator = ", ", prefix = " : ") { it.render() })
            }
            var hasRequiredFields = false
            printBlock {
                var needNewLine = false
                for (field in builder.allFields) {
                    val (newLine, requiredFields) = printFieldInBuilder(field, builder, fieldIsUseless = false)
                    needNewLine = newLine
                    hasRequiredFields = hasRequiredFields || requiredFields
                }
                val hasBackingFields = builder.allFields.any { it.nullable }
                if (needNewLine) {
                    println()
                }
                val buildType = when (builder) {
                    is LeafBuilder<*, *, *> -> builder.implementation.element.render()
                    is IntermediateBuilder -> builder.materializedElement!!.withStarArgs().render()
                }
                if (builder is LeafBuilder<*, *, *> && builder.implementation.isPublic) {
                    println("@OptIn(", implementationDetailAnnotation.render(), "::class)")
                }
                if (builder.parents.isNotEmpty()) {
                    print("override ")
                }
                print("fun build(): ", buildType)
                if (builder is LeafBuilder<*, *, *>) {
                    printBlock {
                        println("return ${builder.implementation.render()}(")
                        withIndent {
                            for (field in builder.allFields) {
                                if (field.invisibleField) continue
                                printFieldReferenceInImplementationConstructorCall(field)
                                println(",")
                            }
                        }
                        println(")")
                    }
                    if (hasBackingFields) {
                        println()
                    }
                } else {
                    println()
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
            if (builder is LeafBuilder<*, *, *>) {
                println()
                @Suppress("UNCHECKED_CAST")
                printDslBuildFunction(builder as LeafBuilder<BuilderField, Element, Implementation>, hasRequiredFields)

                if (builder.wantsCopy) {
                    println()
                    printDslBuildCopyFunction(builder, hasRequiredFields)
                }
            }
        }
    }

    private fun lambdaParameterForBuilderFunction(builder: Builder<BuilderField, Element>, hasRequiredFields: Boolean) =
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

    private fun builderFunctionName(builder: LeafBuilder<BuilderField, Element, Implementation>) =
        "build" + builder.implementation.run { name?.removePrefix(namePrefix) ?: element.name }

    private fun ImportCollectingPrinter.printDslBuildFunction(
        builder: LeafBuilder<BuilderField, Element, Implementation>,
        hasRequiredFields: Boolean,
    ) {
        val isEmpty = builder.allFields.isEmpty()
        if (!isEmpty) {
            println("@OptIn(", experimentalContractsAnnotation.render(), "::class)")
        } else if (builder.implementation.isPublic) {
            println("@OptIn(", implementationDetailAnnotation.render(), "::class)")
        }
        val initParameter = if (isEmpty) null else lambdaParameterForBuilderFunction(builder, hasRequiredFields)
        printFunctionWithBlockBody(
            name = builderFunctionName(builder),
            parameters = listOfNotNull(initParameter),
            returnType = builder.implementation.element,
            typeParameters = builder.implementation.element.params,
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
            if (isEmpty) {
                println(builder.implementation.render(), "()")
            } else {
                println(builder.render(), "().apply(init).build()")
            }
        }
    }

    private fun BuilderField.needBackingField(fieldIsUseless: Boolean) =
        !nullable && origin !is ListField && if (fieldIsUseless) {
            implementationDefaultStrategy?.defaultValue == null
        } else {
            defaultValueInBuilder == null
        }

    private fun BuilderField.needNotNullDelegate(fieldIsUseless: Boolean) =
        needBackingField(fieldIsUseless) && (typeRef == StandardTypes.boolean || typeRef == StandardTypes.int)

    private fun ImportCollectingPrinter.printFieldInBuilder(
        field: BuilderField,
        builder: Builder<BuilderField, Element>,
        fieldIsUseless: Boolean,
    ): Pair<Boolean, Boolean> {
        if (
            field.implementationDefaultStrategy?.withGetter == true
            && !fieldIsUseless || field.invisibleField
        ) return false to false
        if (field.origin is ListField) {
            @Suppress("UNCHECKED_CAST")
            printFieldListInBuilder(field.origin as ElementField, builder, fieldIsUseless)
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
        builder: Builder<BuilderField, Element>,
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
        builder: Builder<BuilderField, Element>,
        fieldIsUseless: Boolean,
    ) {
        printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
        printModifiers(builder, field, fieldIsUseless)
        print("val ", field.name, ": ", actualTypeOfField(field).render())
        if (builder is LeafBuilder<*, *, *>) {
            print(" = mutableListOf()")
        }
        println()
    }

    private fun ImportCollectingPrinter.printModifiers(builder: Builder<BuilderField, Element>, field: AbstractField<*>, fieldIsUseless: Boolean) {
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
            (field as BuilderField).needBackingField(fieldIsUseless) &&
            !fieldIsUseless &&
            !field.needNotNullDelegate(fieldIsUseless = false)
        ) {
            print("lateinit ")
        }
    }

    private fun ImportCollectingPrinter.printDslBuildCopyFunction(
        builder: LeafBuilder<BuilderField, Element, Implementation>,
        hasRequiredFields: Boolean,
    ) {
        val optIns = builder.allFields
            .filter { !it.invisibleField }
            .mapNotNullTo(mutableSetOf(experimentalContractsAnnotation)) { it.optInAnnotation }
        println("@OptIn(", optIns.joinToString { "${it.render()}::class" }, ")")
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
