/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import java.io.File

internal val END_LINE = "\n"

private fun String.shift(): String {
    return this.split(END_LINE).joinToString(separator = END_LINE) { if (it.isEmpty()) it else "    $it" }
}

internal abstract class AnnotatedAndDocumented {
    protected abstract var doc: String?
    protected abstract val annotations: MutableList<String>
    private var additionalDoc: String? = null

    fun addDoc(doc: String) {
        if (this.doc == null) {
            this.doc = doc
        } else {
            this.doc += "$END_LINE$doc"
        }
    }

    fun addAnnotation(annotation: String) {
        annotations += annotation
    }

    fun setAdditionalDoc(doc: String) {
        additionalDoc = doc
    }

    fun StringBuilder.printDocumentationAndAnnotations(forceMultiLineDoc: Boolean = false) {
        if (doc != null) {
            appendLine(doc!!.printAsDoc(forceMultiLineDoc))
        }

        if (annotations.isNotEmpty()) {
            appendLine(annotations.joinToString(separator = END_LINE) { "@$it" })
        }

        if (additionalDoc != null) {
            appendLine("// $additionalDoc")
        }
    }

    private fun String.printAsDoc(forceMultiLine: Boolean = false): String {
        if (this.contains(END_LINE) || forceMultiLine) {
            return this.split(END_LINE).joinToString(
                separator = END_LINE, prefix = "/**$END_LINE", postfix = "$END_LINE */"
            ) { if (it.isEmpty()) " *" else " * $it" }
        }
        return "/** $this */"
    }
}

internal data class FileDescription(
    private val suppresses: MutableList<String> = mutableListOf(),
    private val imports: MutableList<String> = mutableListOf(),
    val classes: List<ClassDescription>
) {
    fun addSuppress(suppress: String) {
        suppresses += suppress
    }

    fun addImport(newImport: String) {
        imports += newImport
    }

    override fun toString(): String {
        return buildString {
            appendLine(File("license/COPYRIGHT_HEADER.txt").readText())
            appendLine()
            appendLine("// Auto-generated file. DO NOT EDIT!")
            appendLine()

            if (suppresses.isNotEmpty()) {
                appendLine(suppresses.joinToString(separator = ", ", prefix = "@file:Suppress(", postfix = ")") { "\"$it\"" })
                appendLine()
            }

            appendLine("package kotlin")
            appendLine()

            if (imports.isNotEmpty()) {
                appendLine(imports.joinToString(separator = END_LINE) { "import $it" })
                appendLine()
            }

            append(classes.joinToString(separator = END_LINE))
        }
    }
}

internal data class ClassDescription(
    override var doc: String?,
    override val annotations: MutableList<String>,
    var isFinal: Boolean = false,
    val name: String,
    var constructorArg: MethodParameter? = null,
    val companionObject: CompanionObjectDescription,
    val methods: List<MethodDescription>
) : AnnotatedAndDocumented() {
    override fun toString(): String {
        return buildString {
            this.printDocumentationAndAnnotations()

            append("public ")
            if (isFinal) append("final ")
            appendLine("class $name private constructor(${constructorArg?.toString() ?: ""}) : Number(), Comparable<$name> {")
            appendLine(companionObject.toString().shift())
            appendLine(methods.joinToString(separator = END_LINE + END_LINE) { it.toString().shift() })
            appendLine("}")
        }
    }
}

internal data class CompanionObjectDescription(
    override var doc: String? = null,
    override val annotations: MutableList<String> = mutableListOf(),
    var isPublic: Boolean = false,
    val properties: List<PropertyDescription>
) : AnnotatedAndDocumented() {
    override fun toString(): String {
        return buildString {
            printDocumentationAndAnnotations()
            if (isPublic) append("public ")
            appendLine("companion object {")
            appendLine(properties.joinToString(separator = END_LINE + END_LINE) { it.toString().shift() })
            appendLine("}")
        }
    }
}

internal data class MethodSignature(
    var isExternal: Boolean = false,
    val visibility: MethodVisibility = MethodVisibility.PUBLIC,
    var isOverride: Boolean = false,
    var isInline: Boolean = false,
    var isInfix: Boolean = false,
    var isOperator: Boolean = false,
    val name: String,
    val arg: MethodParameter?,
    val returnType: String
) {

    override fun toString(): String {
        return buildString {
            if (isExternal) append("external ")
            append("${visibility.name.lowercase()} ")
            if (isOverride) append("override ")
            if (isInline) append("inline ")
            if (isInfix) append("infix ")
            if (isOperator) append("operator ")
            append("fun $name(${arg ?: ""}): $returnType")
        }
    }
}

internal enum class MethodVisibility {
    PUBLIC, INTERNAL, PRIVATE
}

internal data class MethodParameter(val name: String, val type: String) {
    fun getTypeAsPrimitive(): PrimitiveType = PrimitiveType.valueOf(type.uppercase())

    override fun toString(): String {
        return "$name: $type"
    }
}

internal data class MethodDescription(
    override var doc: String?,
    override val annotations: MutableList<String> = mutableListOf(),
    val signature: MethodSignature,
    private var body: String? = null
) : AnnotatedAndDocumented() {
    override fun toString(): String {
        return buildString {
            printDocumentationAndAnnotations()
            append(signature)
            append(body ?: "")
        }
    }

    fun String.addAsSingleLineBody(bodyOnNewLine: Boolean = false) {
        val skip = if (bodyOnNewLine) "$END_LINE\t" else ""
        body = " = $skip$this"
    }

    fun String.addAsMultiLineBody() {
        body = " {$END_LINE${this.shift()}$END_LINE}"
    }
}

internal data class PropertyDescription(
    override var doc: String?,
    override val annotations: MutableList<String> = mutableListOf(),
    val name: String,
    val type: String,
    val value: String
) : AnnotatedAndDocumented() {
    override fun toString(): String {
        return buildString {
            printDocumentationAndAnnotations(forceMultiLineDoc = true)
            append("public const val $name: $type = $value")
        }
    }
}
