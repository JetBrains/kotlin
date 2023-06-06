/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.abi.impl.AbiFunctionImpl.Companion.BITS_ENOUGH_FOR_STORING_PARAMETERS_COUNT
import org.jetbrains.kotlin.library.abi.impl.AbiRendererImpl.RenderedTopLevelDeclarations.printNestedDeclarationsInProperOrder as printTopLevelDeclarations
import kotlin.text.Appendable

@ExperimentalLibraryAbiReader
internal class AbiRendererImpl(
    private val libraryAbi: LibraryAbi,
    private val settings: AbiRenderingSettings,
    private val output: Appendable
) {
    fun render() {
        printHeader()

        if (settings.renderDeclarations)
            printTopLevelDeclarations(libraryAbi.topLevelDeclarations, Printer(output, settings))
    }

    private fun printHeader() {
        output.appendLine(
            """
                // Rendering settings:
                // - Signature version: ${settings.renderedSignatureVersion.versionNumber}
                // - Show manifest properties: ${settings.renderManifest}
                // - Show declarations: ${settings.renderDeclarations}
               
                // Library unique name: <${libraryAbi.uniqueName}>
            """.trimIndent()
        )

        if (settings.renderManifest) {
            with(libraryAbi.manifest) {
                listOfNotNull(
                    platform?.let { "Platform" to it },
                    nativeTargets.takeIf { it.isNotEmpty() }?.let { "Native targets" to it.joinToString(separator = ", ") },
                    compilerVersion?.let { "Compiler version" to it },
                    abiVersion?.let { "ABI version" to it },
                    libraryVersion?.let { "Library version" to it },
                    irProviderName?.let { "IR provider" to it }
                ).forEach { (name, value) ->
                    output.append("// ").append(name).append(": ").appendLine(value)
                }
            }
        }
    }

    private class Printer(private val output: Appendable, private val settings: AbiRenderingSettings) {
        private var indent = 0u

        inline fun indented(block: () -> Unit) {
            indent++
            try {
                block()
            } finally {
                indent--
            }
        }

        fun printDeclaration(renderedDeclaration: RenderedDeclaration<*>, printOpeningBrace: Boolean = false): Unit = with(output) {
            appendIndent()
            append(renderedDeclaration.text)
            if (printOpeningBrace) append(" {")
            appendSignature(renderedDeclaration.declaration)
            appendLine()
        }

        fun printClosingBrace(): Unit = with(output) {
            appendIndent()
            appendLine('}')
        }

        private fun appendIndent() {
            for (i in 0u until indent) output.append(settings.indentationString)
        }

        private fun appendSignature(declaration: AbiDeclaration) {
            output.append(" // ")
            output.append(
                declaration.signatures[settings.renderedSignatureVersion]
                    ?: settings.whenSignatureNotFound(declaration, settings.renderedSignatureVersion)
            )
        }
    }

    private abstract class RenderedDeclarationContainerKind<T : AbiDeclarationContainer> {
        /**
         * Determines the relative order of the given [renderedDeclaration] to put it upper or lower in the renderer's output.
         * The declarations of different kinds (ex: a class and a function) should always get a different order index.
         */
        protected abstract fun orderByDeclarationKind(renderedDeclaration: RenderedDeclaration<*>): Int

        fun printNestedDeclarationsInProperOrder(container: T, printer: Printer) {
            container.declarations.mapAndSort(
                /**
                 * Always sort declarations in a strictly specific order before printing them to make the output
                 * be unaffected by the actual serialization order:
                 *   1. by declaration kind, see [orderByDeclarationKind]
                 *   2. by a fully-qualified name of the declaration
                 *   3. by an additional ordering factor #1, see implementations of [RenderedDeclaration.additionalOrderingFactor1]
                 *   4. by the text of the rendered declaration (excluding signatures!)
                 *   5. by an additional ordering factor #2, see implementations of [RenderedDeclaration.additionalOrderingFactor2]
                 */
                compareBy(
                    ::orderByDeclarationKind,
                    { it.declaration.qualifiedName },
                    RenderedDeclaration<*>::additionalOrderingFactor1,
                    RenderedDeclaration<*>::text,
                    RenderedDeclaration<*>::additionalOrderingFactor2
                ),
                RenderedDeclaration.Companion::createFor
            ).forEach { it.print(printer) }
        }
    }

    private object RenderedTopLevelDeclarations : RenderedDeclarationContainerKind<AbiTopLevelDeclarations>() {
        /**
         * When printing top-level declarations, the following order is used:
         *   1. classes
         *   2. properties
         *   3. functions
         */
        override fun orderByDeclarationKind(renderedDeclaration: RenderedDeclaration<*>) =
            when (renderedDeclaration.declaration) {
                is AbiClass -> 1
                is AbiProperty -> 2
                is AbiFunction -> 3
                else -> 4 // Normally, other types of declarations should not appear as top-level declarations.
            }
    }

    private sealed class RenderedDeclaration<T : AbiDeclaration>(val declaration: T, val text: String) {
        open val additionalOrderingFactor1: Int get() = 0
        open val additionalOrderingFactor2: String get() = ""

        abstract fun print(printer: Printer)

        companion object {
            fun createFor(declaration: AbiDeclaration): RenderedDeclaration<*> = when (declaration) {
                is AbiFunction -> RenderedFunction(declaration)
                is AbiProperty -> RenderedProperty(declaration)
                is AbiClass -> RenderedClass(declaration)
                is AbiEnumEntry -> RenderedEnumEntry(declaration)
            }

            fun StringBuilder.appendModalityOf(declaration: AbiDeclarationWithModality) {
                append(declaration.modality.name.lowercase()).append(' ')
            }

            fun StringBuilder.appendNameOf(declaration: AbiDeclaration) {
                // For non-top level declarations print only simple declaration's name.
                val isTopLevel = declaration.qualifiedName.relativeName.nameSegmentsCount == 1
                append(if (isTopLevel) declaration.qualifiedName else declaration.qualifiedName.relativeName.simpleName)
            }

            fun StringBuilder.appendTypeParametersOf(container: AbiTypeParametersContainer) {
                if (container.typeParameters.isNotEmpty()) {
                    container.typeParameters.joinTo(this, separator = ", ", prefix = "<", postfix = ">") { typeParameter ->
                        appendTypeParameter(typeParameter)
                    }
                    append(' ')
                }
            }

            private fun StringBuilder.appendTypeParameter(typeParameter: AbiTypeParameter): String {
                append('#').append(typeParameter.tag).append(": ")
                if (typeParameter.isReified) append("reified ")
                appendVariance(typeParameter.variance)
                when (typeParameter.upperBounds.size) {
                    0 -> append("kotlin/Any?")
                    1 -> appendType(typeParameter.upperBounds[0])
                    else -> appendSortedTypes(typeParameter.upperBounds, separator = " & ", prefix = "", postfix = "")
                }
                return ""
            }

            fun StringBuilder.appendType(type: AbiType) {
                when (type) {
                    is AbiType.Simple -> when (val classifier = type.classifierReference) {
                        is AbiClassifierReference.ClassReference -> {
                            append(classifier.className)
                            if (type.arguments.isNotEmpty()) {
                                type.arguments.joinTo(this, separator = ", ", prefix = "<", postfix = ">") { typeArgument ->
                                    appendTypeArgument(typeArgument)
                                }
                            }
                            if (type.nullability == AbiTypeNullability.MARKED_NULLABLE) append('?')
                        }
                        is AbiClassifierReference.TypeParameterReference -> {
                            append('#').append(classifier.tag)
                            when (type.nullability) {
                                AbiTypeNullability.MARKED_NULLABLE -> append('?')
                                AbiTypeNullability.NOT_SPECIFIED -> Unit // Do nothing.
                                AbiTypeNullability.DEFINITELY_NOT_NULL -> append("!!")
                            }
                        }
                    }
                    is AbiType.Dynamic -> append("dynamic")
                    is AbiType.Error -> append("error")
                }
            }

            private fun StringBuilder.appendTypeArgument(typeArgument: AbiTypeArgument): String {
                when (typeArgument) {
                    is AbiTypeArgument.StarProjection -> append('*')
                    is AbiTypeArgument.TypeProjection -> {
                        appendVariance(typeArgument.variance)
                        appendType(typeArgument.type)
                    }
                }
                return ""
            }

            private fun StringBuilder.appendVariance(variance: AbiVariance) {
                when (variance) {
                    AbiVariance.INVARIANT -> Unit
                    AbiVariance.IN -> append("in ")
                    AbiVariance.OUT -> append("out ")
                }
            }

            fun StringBuilder.appendSortedTypes(types: List<AbiType>, separator: String, prefix: String, postfix: String) {
                types.mapAndSort(naturalOrder()) { buildString { appendType(it) } }
                    .joinTo(this, separator = separator, prefix = prefix, postfix = postfix)
            }
        }
    }

    private class RenderedClass(declaration: AbiClass) : RenderedDeclaration<AbiClass>(
        declaration = declaration,
        text = buildString {
            appendModalityOf(declaration)
            if (declaration.isInner) append("inner ")
            if (declaration.isValue) append("value ")
            if (declaration.isFunction) append("fun ")
            appendClassKind(declaration.kind)

            // Note: Type parameters are rendered before the class name, exactly as it is done for functions.
            // This is done intentionally for the purpose of unification of the rendering notation for different
            // types of declarations.
            appendTypeParametersOf(declaration)
            appendNameOf(declaration)
            if (declaration.superTypes.isNotEmpty()) {
                appendSortedTypes(declaration.superTypes, separator = ", ", prefix = " : ", postfix = "")
            }
        }
    ) {
        override fun print(printer: Printer) {
            val hasChildren = declaration.declarations.isNotEmpty()
            printer.printDeclaration(this, printOpeningBrace = hasChildren)
            if (hasChildren) {
                printer.indented {
                    printNestedDeclarationsInProperOrder(container = declaration, printer)
                }
                printer.printClosingBrace()
            }
        }

        companion object : RenderedDeclarationContainerKind<AbiClass>() {
            private fun StringBuilder.appendClassKind(classKind: AbiClassKind) {
                append(
                    when (classKind) {
                        AbiClassKind.CLASS -> "class"
                        AbiClassKind.INTERFACE -> "interface"
                        AbiClassKind.OBJECT -> "object"
                        AbiClassKind.ENUM_CLASS -> "enum class"
                        AbiClassKind.ANNOTATION_CLASS -> "annotation class"
                    }
                ).append(' ')
            }

            /**
             * When printing nested declarations inside a class, the following order is used:
             *   1. properties
             *   2. constructors
             *   3. functions
             *   4. nested classes
             *   5. enum entries
             */
            override fun orderByDeclarationKind(renderedDeclaration: RenderedDeclaration<*>) =
                when (val declaration = renderedDeclaration.declaration) {
                    is AbiProperty -> 1
                    is AbiFunction -> if (declaration.isConstructor) 2 else 3
                    is AbiClass -> 4
                    is AbiEnumEntry -> 5
                }
        }
    }

    private class RenderedEnumEntry(declaration: AbiEnumEntry) : RenderedDeclaration<AbiEnumEntry>(
        declaration = declaration,
        text = buildString {
            append("enum entry ")
            appendNameOf(declaration)
        }
    ) {
        override fun print(printer: Printer) = printer.printDeclaration(this)
    }

    private class RenderedProperty(declaration: AbiProperty) : RenderedDeclaration<AbiProperty>(
        declaration = declaration,
        text = buildString {
            appendModalityOf(declaration)
            appendPropertyKind(declaration.kind)
            appendNameOf(declaration)
        }
    ) {
        private val getter = declaration.getter?.let(::RenderedFunction)
        private val setter = declaration.setter?.let(::RenderedFunction)

        /** Delegates to [getter] or [setter], because the property itself knows nothing about value parameters. */
        override val additionalOrderingFactor1 get() = (getter ?: setter)?.additionalOrderingFactor1 ?: 0

        /**
         * Use rendered text of [getter] or [setter], because the rendered text of the property does not include
         * any value parameter information useful for the proper ordering among all properties that share
         * the same qualified name (i.e. among overloaded properties).
         */
        override val additionalOrderingFactor2 get() = (getter ?: setter)?.text.orEmpty()

        override fun print(printer: Printer) {
            printer.printDeclaration(this)
            printer.indented {
                getter?.print(printer)
                setter?.print(printer)
            }
        }

        companion object {
            private fun StringBuilder.appendPropertyKind(propertyKind: AbiPropertyKind) {
                append(
                    when (propertyKind) {
                        AbiPropertyKind.VAL -> "val"
                        AbiPropertyKind.CONST_VAL -> "const val"
                        AbiPropertyKind.VAR -> "var"
                    }
                ).append(' ')
            }
        }
    }

    private class RenderedFunction(declaration: AbiFunction) : RenderedDeclaration<AbiFunction>(
        declaration = declaration,
        text = buildString {
            if (!declaration.isConstructor || declaration.modality != AbiModality.FINAL) appendModalityOf(declaration)
            if (declaration.isSuspend) append("suspend ")
            if (declaration.isInline) append("inline ")
            append(if (declaration.isConstructor) "constructor " else "fun ")
            appendTypeParametersOf(declaration)
            appendIrregularValueParametersOf(declaration)
            appendNameOf(declaration)
            appendRegularValueParametersOf(declaration)
            appendReturnTypeOf(declaration)
        }
    ) {
        /**
         * Determines the relative order of a function to put it upper or lower in the renderer's output:
         * - Functions without extension receiver go above functions with an extension receiver.
         * - Functions without context receivers go above functions with context receivers.
         * - The more regular value parameters function has, the lower it goes.
         * - Same among functions with context receiver parameters.
         */
        override val additionalOrderingFactor1: Int
            get() {
                val extensionReceivers = if (declaration.hasExtensionReceiverParameter) 1 else 0
                val contextReceivers = declaration.contextReceiverParametersCount
                val regularParameters = declaration.valueParameters.size - extensionReceivers - contextReceivers
                return (((contextReceivers shl 1) or extensionReceivers) shl BITS_ENOUGH_FOR_STORING_PARAMETERS_COUNT) or regularParameters
            }

        override fun print(printer: Printer) = printer.printDeclaration(this)

        companion object {
            private fun StringBuilder.appendIrregularValueParametersOf(function: AbiFunction) {
                if (function.contextReceiverParametersCount > 0)
                    function.valueParameters
                        .asSequence()
                        .apply { if (function.hasExtensionReceiverParameter) drop(1) }
                        .take(function.contextReceiverParametersCount)
                        .joinTo(this, separator = ", ", prefix = "context(", postfix = ") ") { valueParameter ->
                            appendValueParameter(valueParameter)
                        }

                if (function.hasExtensionReceiverParameter) {
                    append('(')
                    appendValueParameter(function.valueParameters[0])
                    append(").")
                }
            }

            private fun StringBuilder.appendRegularValueParametersOf(function: AbiFunction) {
                val skippedParametersCount = (if (function.hasExtensionReceiverParameter) 1 else 0) +
                        function.contextReceiverParametersCount

                function.valueParameters
                    .asSequence()
                    .drop(skippedParametersCount)
                    .joinTo(this, separator = ", ", prefix = "(", postfix = ")") { valueParameter ->
                        appendValueParameter(valueParameter)
                    }
            }

            private fun StringBuilder.appendValueParameter(valueParameter: AbiValueParameter): String {
                if (valueParameter.isNoinline) append("noinline ")
                if (valueParameter.isCrossinline) append("crossinline ")
                appendType(valueParameter.type)
                if (valueParameter.isVararg) append("...")
                if (valueParameter.hasDefaultArg) append(" =...")
                return ""
            }

            private fun StringBuilder.appendReturnTypeOf(function: AbiFunction) {
                function.returnType?.let { returnType -> append(": ").appendType(returnType) }
            }
        }
    }

    companion object {
        private inline fun <T, R : Any> List<T>.mapAndSort(comparator: Comparator<R>, transform: (T) -> R): List<R> {
            if (isEmpty()) return emptyList()

            val result = ArrayList<R>(size)
            mapTo(result, transform)
            result.sortWith(comparator)
            return result
        }
    }
}
