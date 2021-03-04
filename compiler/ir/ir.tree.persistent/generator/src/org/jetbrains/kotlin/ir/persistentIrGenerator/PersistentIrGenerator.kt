/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

import java.io.File
import java.lang.StringBuilder
import java.time.Year

internal interface R {
    fun text(t: String): R

    fun indent(fn: R.() -> Unit): R

    fun import(fqn: String): R
}

internal typealias E = R.() -> R

internal class Field(val name: String, val type: E, val lateinit: Boolean = false, val notEq: String = "!==")

internal object PersistentIrGenerator {

    // Imports

    val ClassDescriptor: E = descriptorType("ClassDescriptor")
    val DeclarationDescriptor: E = descriptorType("DeclarationDescriptor")
    val ClassConstructorDescriptor: E = descriptorType("ClassConstructorDescriptor")
    val DescriptorVisibility = descriptorType("DescriptorVisibility")

    val IrDeclaration = irDeclaration("IrDeclaration")
    val IrDeclarationOrigin = irDeclaration("IrDeclarationOrigin")
    val IrDeclarationParent = irDeclaration("IrDeclarationParent")
    val IrAnonymousInitializer = irDeclaration("IrAnonymousInitializer")
    val IrClass = irDeclaration("IrClass")
    val IrTypeParameter = irDeclaration("IrTypeParameter")
    val IrValueParameter = irDeclaration("IrValueParameter")
    val MetadataSource = irDeclaration("MetadataSource")
    val IrAttributeContainer = irDeclaration("IrAttributeContainer")

    val IrConstructorCall = irExpression("IrConstructorCall")
    val IrBody = irExpression("IrBody")
    val IrBlockBody = irExpression("IrBlockBody")
    val IrExpressionBody = irExpression("IrExpressionBody")

    val IrAnonymousInitializerSymbol = irSymbol("IrAnonymousInitializerSymbol")
    val IrClassSymbol = irSymbol("IrClassSymbol")

    val AnonymousInitializerCarrier = irCarrier("AnonymousInitializerCarrier")
    val Carrier = irCarrier("Carrier")

    val ObsoleteDescriptorBasedAPI = import("ObsoleteDescriptorBasedAPI", "org.jetbrains.kotlin.ir")

    val IrType = import("IrType", "org.jetbrains.kotlin.ir.types")

    val IrPropertySymbol = irSymbol("IrPropertySymbol")

    // Constructor parameters

    val startOffset = +"override val startOffset: Int"
    val endOffset = +"override val endOffset: Int"
    val origin = +"origin: " + IrDeclarationOrigin
    val isStatic = +"override val isStatic: Boolean"
    val name = +"override val name: " + import("Name", "org.jetbrains.kotlin.name")
    val kind = +"override val kind: " + descriptorType("ClassKind")
    val visibility = +"visibility: " + DescriptorVisibility
    val modality = +"modality: " + descriptorType("Modality")
    val isCompanion = +"override val isCompanion: Boolean = false"
    val isInner = +"override val isInner: Boolean = false"
    val isData = +"override val isData: Boolean = false"
    val isExternal = +"override val isExternal: Boolean"
    val isFinal = +"override val isFinal: Boolean"
    val isInline = +"override val isInline: Boolean"
    val isExpect = +"override val isExpect: Boolean"
    val isFun = +"override val isFun: Boolean = false"
    val source = +"override val source: " + descriptorType("SourceElement") + " = SourceElement.NO_SOURCE"
    val returnType = +"returnType: " + IrType
    val isPrimary = +"override val isPrimary: Boolean"
    val containerSource = +"override val containerSource: " + import("DeserializedContainerSource", "org.jetbrains.kotlin.serialization.deserialization.descriptors") + "?"

    val irFactory = +"override val factory: PersistentIrFactory"

    val initBlock = +"init " + block(
        +"symbol.bind(this)"
    )

    // Fields
    val lastModified = +"override var lastModified: Int = factory.stageController.currentStage"
    val loweredUpTo = +"override var loweredUpTo: Int = factory.stageController.currentStage"
    val values = +"override var values: Array<" + Carrier + ">? = null"
    val createdOn = +"override val createdOn: Int = factory.stageController.currentStage"

    val parentField = +"override var parentField: " + IrDeclarationParent + "? = null"
    val originField = +"override var originField: " + IrDeclarationOrigin + " = origin"
    val removedOn = +"override var removedOn: Int = Int.MAX_VALUE"
    val annotationsField = +"override var annotationsField: List<" + IrConstructorCall + "> = emptyList()"

    val commonFields = lines(
        lastModified,
        loweredUpTo,
        values,
        createdOn,
        id,
        parentField,
        originField,
        removedOn,
        annotationsField,
    )

    fun Field.toPersistentField(initializer: E, modifier: String = "override") =
        persistentField(name, type, initializer, lateinit, modifier, notEq = notEq)

    fun Field.toBody() = body(type, lateinit, name)

    // Helpers

    fun baseClasses(name: String, baseClass: String = "Ir$name"): E = lines(
        irDeclaration(baseClass) + "(),",
        +"    PersistentIrDeclarationBase<" + irCarrier("${name}Carrier") + ">,",
        +"    ${name}Carrier",
    )

    fun persistentField(
        name: String,
        type: E,
        initializer: E,
        lateinit: Boolean = false,
        modifier: String = "override",
        isBody: Boolean = false,
        notEq: String = "!==",
    ): E = lines(
        +"override var ${name}Field: " + type + "${if (lateinit) "?" else ""} = " + initializer,
        id,
        +"$modifier var $name: " + type,
        lines(
            +"get() = getCarrier().${name}Field${if (lateinit) "!!" else ""}",
            +"set(v) " + block(
                +"if (${if (lateinit) "getCarrier().${name}Field" else name} $notEq v) " + block(
                    (if (isBody) lines(
                        +"if (v is PersistentIrBodyBase<*>) " + block(
                            +"v.container = this"
                        ),
                        id
                    ) else id) + "setCarrier()",
                    +"${name}Field = v"
                )
            )
        ).indent()
    )

    fun body(bodyType: E, lateinit: Boolean = false, fieldName: String = "body"): E =
        persistentField(fieldName, bodyType, initializer = +"null", lateinit, isBody = true)

    fun descriptor(type: E) = lines(
        +"@" + ObsoleteDescriptorBasedAPI,
        +"override val descriptor: " + type,
        +"    get() = symbol.descriptor"
    )

    fun carriers(name: String, vararg fields: Field): E = lines(
        id,
        +"internal interface ${name}Carrier : DeclarationCarrier" + block(
            *(fields.map { +"val ${it.name}Field: " + it.type + if (it.lateinit) "?" else "" }.toTypedArray()),
            id,
            +"override fun clone(): ${name}Carrier " + block(
                +"return ${name}CarrierImpl(",
                arrayOf(
                    +"lastModified",
                    +"parentField",
                    +"originField",
                    +"annotationsField",
                    *(fields.map { +"${it.name}Field" }.toTypedArray())
                ).join(separator = ",\n").indent(),
                +")",
            )
        ),
        id,
        +"internal class ${name}CarrierImpl(",
        arrayOf(
            +"override val lastModified: Int",
            +"override val parentField: " + IrDeclarationParent + "?",
            +"override val originField: " + IrDeclarationOrigin,
            +"override val annotationsField: List<" + IrConstructorCall + ">",
            *(fields.map { +"override val ${it.name}Field: " + it.type + if (it.lateinit) "?" else "" }.toTypedArray()),
        ).join(separator = ",\n").indent(),
        +") : ${name}Carrier",
        id,
    )

    fun lines(vararg fn: E): E = fn.join(separator = "\n")

    fun block(vararg fn: E): E = lines(+"{", { indent { lines(*fn)() } }, +"}")

    fun blockSpaced(vararg fn: E): E {
        return block(*(fn.flatMap { listOf(id, it) }.toTypedArray()))
    }

    fun import(name: String, pkg: String): E = { import("$pkg.$name").text(name) }

    fun descriptorType(name: String): E = import(name, "org.jetbrains.kotlin.descriptors")

    fun irDeclaration(name: String): E = import(name, "org.jetbrains.kotlin.ir.declarations")

    fun irExpression(name: String): E = import(name, "org.jetbrains.kotlin.ir.expressions")

    fun irCarrier(name: String): E = import(name, "org.jetbrains.kotlin.ir.declarations.persistent.carriers")

    fun irSymbol(name: String): E = import(name, "org.jetbrains.kotlin.ir.symbols")

    infix operator fun E.plus(e: E?): E = { this@plus(); e.safe()() }

    infix operator fun E.plus(e: String): E = this + (+e)

    operator fun String.unaryPlus(): E = { text(this@unaryPlus) }

    val id: E get() = { this }

    fun E?.safe(): E = this ?: id

    fun Array<out E>.join(prefix: String = "", separator: String = "", suffix: String = ""): E =
        join(+prefix, +separator, +suffix)

    fun Array<out E>.join(prefix: E = id, separator: E = id, suffix: E = id): E {
        if (this.isEmpty()) return id
        return prefix + interleaveWith(separator) + suffix
    }

    fun Array<out E>.interleaveWith(b: E): E {
        return {
            this@interleaveWith.forEachIndexed { i, e ->
                if (i != 0) b()
                e()
            }
            this
        }
    }

    fun Boolean.ifTrue(s: String): E = if (this) +s else id

    fun type(name: E, vararg parameters: E, isNullable: Boolean = false): E =
        name + parameters.join("<", ", ", ">") + isNullable.ifTrue("?")

    fun E.indent(): E = {
        val self = this@indent
        indent {
            self()
        }
    }

    fun renderFile(pkg: String, fn: R.() -> R): String {
        val sb = StringBuilder()
        val imports: MutableSet<String> = mutableSetOf()

        val renderer = object : R {
            var currentIndent = ""

            var atLineStart = true

            override fun text(t: String): R {
                if (t.isEmpty()) return this

                if (atLineStart) {
                    sb.append(currentIndent)
                    atLineStart = false
                }

                val cr = t.indexOf('\n')
                if (cr >= 0) {
                    sb.append(t.substring(0, cr + 1))
                    atLineStart = true
                    text(t.substring(cr + 1))
                } else {
                    sb.append(t)
                }

                return this
            }

            override fun indent(fn: R.() -> Unit): R {
                val oldIndent = currentIndent
                currentIndent = "$oldIndent    "
                fn()
                currentIndent = oldIndent

                return this
            }

            override fun import(fqn: String): R {
                imports += fqn

                return this
            }
        }

        renderer.fn()

        var result = File("license/COPYRIGHT_HEADER.txt").readText() + "\n\n" + "package $pkg\n\n"

        result += imports.map { "import $it" }.sorted().joinToString(separator = "\n")
        result += "\n\n"
        result += "// Auto-generated by compiler/ir/ir.tree.persistent/generator/src/org/jetbrains/kotlin/ir/persistentIrGenerator/Main.kt. DO NOT EDIT!\n"
        result += sb.toString()

        result = result.lines().map { if (it.isBlank()) "" else it }.joinToString(separator = "\n")

        return result
    }

    private val prefix = "compiler/ir/ir.tree.persistent/gen/org/jetbrains/kotlin/ir/declarations/persistent/"

    fun writeFile(path: String, content: String) {
        File(prefix + path).writeText(content)
    }
}