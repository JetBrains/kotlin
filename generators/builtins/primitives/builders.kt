/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import java.io.File

private fun String.shift(): String {
    return this.split(END_LINE).joinToString(separator = END_LINE) { if (it.isEmpty()) it else "    $it" }
}

internal fun file(init: FileBuilder.() -> Unit): FileBuilder {
    val file = FileBuilder()
    file.init()
    return file
}

internal interface PrimitiveBuilder {
    fun build(): String

    fun throwIfAlreadyInitialized(arg: Any?, propertyName: String, className: String) {
        if (arg != null) {
            throw AssertionError("Property '$propertyName' for '$className' was already initialized")
        }
    }

    fun throwIfWasNotInitialized(arg: Any?, propertyName: String, className: String) {
        if (arg == null) {
            throw AssertionError("Property '$propertyName' for '$className' wasn't set to its value")
        }
    }

    fun throwNotInitialized(propertyName: String, className: String): Nothing {
        throw AssertionError("Property '$propertyName' for '$className' wasn't initialized to access")
    }
}

internal abstract class AnnotatedAndDocumented {
    private var doc: String? = null
    val annotations: MutableList<String> = mutableListOf()
    var additionalDoc: String? = null

    fun appendDoc(doc: String) {
        if (this.doc == null) {
            this.doc = doc
        } else {
            this.doc += "$END_LINE$doc"
        }
    }

    protected fun StringBuilder.printDocumentationAndAnnotations(forceMultiLineDoc: Boolean = false) {
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

internal class FileBuilder : PrimitiveBuilder {
    private val suppresses: MutableList<String> = mutableListOf()
    private val imports: MutableList<String> = mutableListOf()
    private val classes: MutableList<ClassBuilder> = mutableListOf()

    fun suppress(suppress: String) {
        suppresses += suppress
    }

    fun import(newImport: String) {
        imports += newImport
    }

    fun klass(init: ClassBuilder.() -> Unit): ClassBuilder {
        val classBuilder = ClassBuilder()
        classes += classBuilder.apply(init)
        return classBuilder
    }

    override fun build(): String {
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

            append(classes.joinToString(separator = END_LINE) { it.build() })
        }
    }
}

internal class ClassBuilder : AnnotatedAndDocumented(), PrimitiveBuilder {
    var isFinal: Boolean = false
    var name: String = ""
    private var constructorParam: MethodParameterBuilder? = null
    private var companionObject: CompanionObjectBuilder? = null
    private val methods: MutableList<MethodBuilder> = mutableListOf()

    fun constructorParam(init: MethodParameterBuilder.() -> Unit) {
        throwIfAlreadyInitialized(constructorParam, "constructorParam", "ClassBuilder")
        constructorParam = MethodParameterBuilder().apply(init)
    }

    fun companionObject(init: CompanionObjectBuilder.() -> Unit): CompanionObjectBuilder {
        throwIfAlreadyInitialized(companionObject, "companionObject", "ClassBuilder")
        val companionObjectBuilder = CompanionObjectBuilder()
        companionObject = companionObjectBuilder.apply(init)
        return companionObjectBuilder
    }

    fun method(init: MethodBuilder.() -> Unit): MethodBuilder {
        val methodBuilder = MethodBuilder()
        methods += methodBuilder.apply(init)
        return methodBuilder
    }

    override fun build(): String {
        return buildString {
            this.printDocumentationAndAnnotations()

            append("public ")
            if (isFinal) append("final ")
            appendLine("class $name private constructor(${constructorParam?.build() ?: ""}) : Number(), Comparable<$name> {")

            companionObject?.let { appendLine(it.build().shift()) }
            appendLine(methods.joinToString(separator = END_LINE + END_LINE) { it.build().shift() })
            appendLine("}")
        }
    }
}

internal class CompanionObjectBuilder : AnnotatedAndDocumented(), PrimitiveBuilder {
    var isPublic: Boolean = false
    private val properties: MutableList<PropertyBuilder> = mutableListOf()

    fun property(init: PropertyBuilder.() -> Unit): PropertyBuilder {
        val propertyBuilder = PropertyBuilder()
        properties += propertyBuilder.apply(init)
        return propertyBuilder
    }

    override fun build(): String {
        return buildString {
            printDocumentationAndAnnotations()
            if (isPublic) append("public ")
            appendLine("companion object {")
            appendLine(properties.joinToString(separator = END_LINE + END_LINE) { it.build().shift() })
            appendLine("}")
        }
    }
}

internal class MethodSignatureBuilder : PrimitiveBuilder {
    var isExternal: Boolean = false
    var visibility: MethodVisibility = MethodVisibility.PUBLIC
    var isOverride: Boolean = false
    var isInline: Boolean = false
    var isInfix: Boolean = false
    var isOperator: Boolean = false

    var methodName: String? = null
    private var parameter: MethodParameterBuilder? = null
    var returnType: String? = null

    val parameterName: String
        get() = parameter?.name ?: throwNotInitialized("name", "MethodParameterBuilder")

    val parameterType: String
        get() = parameter?.type ?: throwNotInitialized("type", "MethodParameterBuilder")

    fun parameter(init: MethodParameterBuilder.() -> Unit): MethodParameterBuilder {
        throwIfAlreadyInitialized(parameter, "parameter", "MethodSignatureBuilder")
        val argBuilder = MethodParameterBuilder()
        parameter = argBuilder.apply(init)
        return argBuilder
    }

    override fun build(): String {
        throwIfWasNotInitialized(methodName, "methodName", "MethodSignatureBuilder")
        throwIfWasNotInitialized(returnType, "returnType", "MethodSignatureBuilder")

        return buildString {
            if (isExternal) append("external ")
            append("${visibility.name.lowercase()} ")
            if (isOverride) append("override ")
            if (isInline) append("inline ")
            if (isInfix) append("infix ")
            if (isOperator) append("operator ")
            append("fun $methodName(${parameter?.build() ?: ""}): $returnType")
        }
    }
}

internal enum class MethodVisibility {
    PUBLIC, INTERNAL, PRIVATE
}

internal class MethodParameterBuilder : PrimitiveBuilder {
    var name: String? = null
    var type: String? = null

    override fun build(): String {
        throwIfWasNotInitialized(name, "name", "MethodParameterBuilder")
        throwIfWasNotInitialized(type, "type", "MethodParameterBuilder")
        return "$name: $type"
    }
}

internal class MethodBuilder : AnnotatedAndDocumented(), PrimitiveBuilder {
    private var signature: MethodSignatureBuilder? = null
    private var body: String? = null

    val methodName: String
        get() = signature?.methodName ?: throwNotInitialized("methodName", "MethodSignatureBuilder")

    val returnType: String
        get() = signature?.returnType ?: throwNotInitialized("returnType", "MethodSignatureBuilder")

    val parameterName: String
        get() = signature?.parameterName ?: throwNotInitialized("name", "MethodParameterBuilder")

    val parameterType: String
        get() = signature?.parameterType ?: throwNotInitialized("type", "MethodParameterBuilder")

    fun signature(init: MethodSignatureBuilder.() -> Unit): MethodSignatureBuilder {
        throwIfAlreadyInitialized(signature, "signature", "MethodBuilder")
        val signatureBuilder = MethodSignatureBuilder()
        signature = signatureBuilder.apply(init)
        return signatureBuilder
    }

    fun modifySignature(modify: MethodSignatureBuilder.() -> Unit) {
        throwIfWasNotInitialized(signature, "signature", "MethodBuilder")
        signature!!.apply(modify)
    }

    override fun build(): String {
        throwIfWasNotInitialized(signature, "signature", "MethodBuilder")

        return buildString {
            printDocumentationAndAnnotations()
            append(signature!!.build())
            append(body ?: "")
        }
    }

    fun String.addAsSingleLineBody(bodyOnNewLine: Boolean = false) {
        val skip = if (bodyOnNewLine) "$END_LINE    " else " "
        body = " =$skip$this"
    }

    fun String.addAsMultiLineBody() {
        body = " {$END_LINE${this.shift()}$END_LINE}"
    }
}

internal class PropertyBuilder : AnnotatedAndDocumented(), PrimitiveBuilder {
    var name: String? = null
    var type: String? = null
    var value: String? = null

    override fun build(): String {
        throwIfWasNotInitialized(name, "name", "PropertyBuilder")
        throwIfWasNotInitialized(type, "type", "PropertyBuilder")
        throwIfWasNotInitialized(value, "value", "PropertyBuilder")

        return buildString {
            printDocumentationAndAnnotations(forceMultiLineDoc = true)
            append("public const val $name: $type = $value")
        }
    }
}
