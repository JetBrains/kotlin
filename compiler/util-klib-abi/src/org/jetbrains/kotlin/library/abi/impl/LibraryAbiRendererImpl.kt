/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.library.abi.*
import java.lang.Appendable

internal class AbiRendererImpl(
    private val topLevelDeclarations: AbiTopLevelDeclarations,
    private val settings: AbiRenderingSettings
) {
    private val signatureVersionForOrdering = settings.renderedSignatureVersions[0]

    fun renderTo(output: Appendable) {
        topLevelDeclarations.renderDeclarationContainer(isTopLevel = true, output)
    }

    private fun AbiDeclarationContainer.renderDeclarationContainer(isTopLevel: Boolean, output: Appendable) {
        fun AbiDeclaration.topLevelOrderByDeclarationKind(): Int = when (this) {
            is AbiClass -> 1
            is AbiEnumEntry -> 2
            is AbiProperty -> 3
            is AbiFunction -> if (isConstructor) 4 else 5
            else -> error("Unexpected declaration kind: ${this::class.java}, $this")
        }

        fun AbiDeclaration.nestedOrderByDeclarationKind(): Int = when (this) {
            is AbiProperty -> 1
            is AbiFunction -> if (isConstructor) 2 else 3
            is AbiClass -> 4
            is AbiEnumEntry -> 5
            else -> error("Unexpected declaration kind: ${this::class.java}, $this")
        }

        fun AbiDeclaration.orderByTheFirstSignature(): String =
            signatures[signatureVersionForOrdering] ?: settings.whenSignatureNotFound(this, signatureVersionForOrdering)

        declarations.sortedWith(
            compareBy(
                if (isTopLevel) AbiDeclaration::topLevelOrderByDeclarationKind else AbiDeclaration::nestedOrderByDeclarationKind,
                AbiDeclaration::name,
                AbiDeclaration::orderByTheFirstSignature
            )
        ).forEach { declaration ->
            when (declaration) {
                is AbiClass -> declaration.renderClass(output)
                is AbiEnumEntry -> declaration.renderEnumEntry(output)
                is AbiProperty -> declaration.renderProperty(output)
                is AbiFunction -> declaration.renderFunction(output)
            }
        }
    }

    private fun AbiClass.renderClass(output: Appendable): Unit = renderDeclarationBase(
        output,
        doBeforeSignatures = {
            if (isInner) output.append("inner ")
            if (isValue) output.append("value ")
            if (isFunction) output.append("fun ")
            output.appendClassKind(kind)
        },
        doAfterSignatures = {
            if (superTypes.isNotEmpty()) {
                output.append(" : ")
                val renderedSuperTypes = superTypes.mapTo(ArrayList(superTypes.size)) { it.render() }
                renderedSuperTypes.sort()
                renderedSuperTypes.joinTo(output, separator = ", ")
            }

            if (declarations.isNotEmpty()) {
                output.appendLine(" {")
                indented {
                    renderDeclarationContainer(isTopLevel = false, output)
                }
                output.appendIndent().append('}')
            }

            output.appendLine()
        }
    )

    private fun AbiEnumEntry.renderEnumEntry(output: Appendable) = renderDeclarationBase(
        output,
        doBeforeSignatures = {
            output.append(ENUM_ENTRY_REPRESENTATION)
        }
    )

    private fun AbiFunction.renderFunction(output: Appendable) = renderDeclarationBase(
        output,
        doBeforeSignatures = {
            if (isSuspend) output.append("suspend ")
            if (isInline) output.append("inline ")
            output.append(if (isConstructor) "constructor" else "fun")
            output.appendValueParameterFlags(valueParameters)
        }
    )

    private fun AbiProperty.renderProperty(output: Appendable) = renderDeclarationBase(
        output,
        doBeforeSignatures = {
            output.appendPropertyKind(kind)
        },
        doAfterSignatures = {
            output.appendLine()
            indented {
                getter?.renderFunction(output)
                setter?.renderFunction(output)
            }
        }
    )

    private inline fun <T : AbiDeclaration> T.renderDeclarationBase(
        output: Appendable,
        doBeforeSignatures: T.() -> Unit,
        doAfterSignatures: T.() -> Unit = { output.appendLine() },
    ) {
        output.appendIndent()
        if (this is AbiPossiblyTopLevelDeclaration && needToRenderModality) output.appendModality(modality).append(' ')
        doBeforeSignatures()
        output.append(' ').appendSignatures(this)
        doAfterSignatures()
    }

    private fun AbiType.render(): String = buildString { render(this) }

    private fun AbiType.render(output: StringBuilder) {
        when (this) {
            is AbiType.Simple -> when (val classifier = classifier) {
                is AbiClassifier.Class -> {
                    output.append(classifier.className)
                    if (arguments.isNotEmpty()) {
                        output.append('<')
                        arguments.forEachIndexed { index, typeArgument ->
                            if (index > 0) output.append(", ")
                            typeArgument.render(output)
                        }
                        output.append('>')
                    }
                    if (nullability == AbiTypeNullability.MARKED_NULLABLE) output.append('?')
                }
                is AbiClassifier.TypeParameter -> {
                    if (nullability == AbiTypeNullability.DEFINITELY_NOT_NULL) output.append('{')
                    output.append(classifier.declaringClassName).append('#').append(classifier.index)
                    when (nullability) {
                        AbiTypeNullability.MARKED_NULLABLE -> output.append('?')
                        AbiTypeNullability.NOT_SPECIFIED -> Unit // Do nothing.
                        AbiTypeNullability.DEFINITELY_NOT_NULL -> output.append(" & kotlin/Any}")
                    }
                }
            }
            is AbiType.Dynamic -> output.append("dynamic")
            is AbiType.Error -> output.append("<error>")
        }
    }

    private fun AbiTypeArgument.render(output: StringBuilder) {
        when (this) {
            is AbiTypeArgument.StarProjection -> output.append('*')
            is AbiTypeArgument.RegularProjection -> {
                when (projectionKind) {
                    AbiVariance.INVARIANT -> Unit
                    AbiVariance.IN_VARIANCE -> output.append("in ")
                    AbiVariance.OUT_VARIANCE -> output.append("out ")
                }
                type.render(output)
            }
        }
    }

    private var indent = 0u

    private inline fun indented(block: () -> Unit) {
        indent++
        try {
            block()
        } finally {
            indent--
        }
    }

    private fun Appendable.appendIndent(): Appendable {
        for (i in 0u until indent) append("    ")
        return this
    }

    private val AbiPossiblyTopLevelDeclaration.needToRenderModality: Boolean
        get() = this !is AbiFunction || !isConstructor || modality != AbiModality.FINAL

    private fun Appendable.appendModality(modality: AbiModality): Appendable = append(modality.name.lowercase())

    private fun Appendable.appendClassKind(classKind: AbiClassKind): Appendable =
        append(
            when (classKind) {
                AbiClassKind.CLASS -> "class"
                AbiClassKind.INTERFACE -> "interface"
                AbiClassKind.OBJECT -> "object"
                AbiClassKind.ENUM_CLASS -> "enum class"
                AbiClassKind.ANNOTATION_CLASS -> "annotation class"
            }
        )

    private fun Appendable.appendPropertyKind(propertyKind: AbiPropertyKind): Appendable =
        append(
            when (propertyKind) {
                AbiPropertyKind.VAL -> "val"
                AbiPropertyKind.CONST_VAL -> "const val"
                AbiPropertyKind.VAR -> "var"
            }
        )

    private fun Appendable.appendSignatures(declaration: AbiDeclaration): Appendable {
        settings.renderedSignatureVersions.joinTo(this, separator = ", ") { signatureVersion ->
            declaration.signatures[signatureVersion] ?: settings.whenSignatureNotFound(declaration, signatureVersion)
        }
        return this
    }

    private fun Appendable.appendValueParameterFlags(valueParameters: List<AbiValueParameter>): Appendable {
        var hadMeaningfulFlagsToRender = false

        valueParameters.forEachIndexed { index, valueParameter ->
            val flags = listOfNotNull(
                "default_arg".takeIf { valueParameter.hasDefaultArg },
                "noinline".takeIf { valueParameter.isNoinline },
                "crossinline".takeIf { valueParameter.isCrossinline }
            )
            if (flags.isEmpty()) return@forEachIndexed

            append(if (hadMeaningfulFlagsToRender) ' ' else '[')
            append("$index:")
            flags.joinTo(this, separator = ",")
            hadMeaningfulFlagsToRender = true
        }

        if (hadMeaningfulFlagsToRender)
            append(']')

        return this
    }

    companion object {
        private const val ENUM_ENTRY_REPRESENTATION = "enum entry"
    }
}
