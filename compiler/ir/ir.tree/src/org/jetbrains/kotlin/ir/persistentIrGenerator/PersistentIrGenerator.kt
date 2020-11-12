/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

import java.io.File
import java.lang.IllegalStateException
import java.lang.StringBuilder

internal interface R {
    fun text(t: String): R

    fun indent(fn: R.() -> Unit): R

    fun import(fqn: String): R
}

internal typealias E = R.() -> R

internal enum class FieldKind {
    REQUIRED, OPTIONAL, REPEATED
}

internal class Proto(
    val protoPrefix: String?, // null if flag, proto type if not
    val entityName: String,   // what to deserialize
    val protoType: E,         // what type ProtoBuf generates
    val irType: E,            // what Ir class this maps to
    val fieldKind: FieldKind = FieldKind.OPTIONAL
)

internal class Field(val name: String, val type: E, val proto: Proto? = null, val lateinit: Boolean = false)

internal object PersistentIrGenerator {

    private val protoPackage = "org.jetbrains.kotlin.backend.common.serialization.proto"
    val carrierPackage = "org.jetbrains.kotlin.ir.declarations.persistent.carriers"

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
    val IrVariable = irDeclaration("IrVariable")

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
        persistentField(name, type, initializer, lateinit, modifier)

    fun Field.toBody() = body(type, lateinit, name)

    // Proto types

    val protoValueParameterType = import("IrValueParameter", protoPackage, "ProtoIrValueParameter")
    val protoTypeParameterType = import("IrTypeParameter", protoPackage, "ProtoIrTypeParameter")
    val protoType = import("IrType", protoPackage, "ProtoIrType")
    val protoVariable = import("IrVariable", protoPackage, "ProtoIrVariable")
    val protoIrConstructorCall = import("IrConstructorCall", protoPackage, "ProtoIrConstructorCall")

    val bodyProtoType = Proto("int32", "body", +"Int", IrBody)
    val blockBodyProtoType = Proto("int32", "blockBody", +"Int", IrBlockBody)
    val expressionBodyProtoType = Proto("int32", "expressionBody", +"Int", IrExpressionBody)
    val valueParameterProtoType = Proto("IrValueParameter", "valueParameter", protoValueParameterType, IrValueParameter)
    val valueParameterListProtoType = Proto("IrValueParameter", "valueParametersList", protoValueParameterType, IrValueParameter, fieldKind = FieldKind.REPEATED)
    val typeParameterListProtoType = Proto("IrTypeParameter", "typeParametersList", protoTypeParameterType, IrTypeParameter, fieldKind = FieldKind.REPEATED)
    val superTypeListProtoType = Proto("int32", "superTypesList", +"Int", IrType, fieldKind = FieldKind.REPEATED)
    val typeProtoType = Proto("IrType", "type", protoType, IrType)
    val symbolProtoType = Proto("int64", "symbol", +"Long", +"Nothing")
    val symbolListProtoType = Proto("int64", "symbolList", +"Long", +"Nothing", fieldKind = FieldKind.REPEATED)
    val variableProtoType = Proto("IrVariable", "variable", protoVariable, IrVariable)

    val visibilityProto = Proto(null, "visibility", +"Long", DescriptorVisibility)
    val modalityProto = Proto(null, "modality", +"Long", descriptorType("Modality"))


    val protoMessages = mutableListOf<String>()

    fun addCarrierProtoMessage(carrierName: String, vararg fields: Field) {
        val protoFields = mutableListOf(
            "required int32 lastModified",
            "optional int64 parentSymbol",
            "optional int32 origin",
            "repeated IrConstructorCall annotation"
        )

        protoFields += fields.mapNotNull { f ->
            f.proto?.protoPrefix?.let { p ->
                val modifier = f.proto.fieldKind.toString().toLowerCase()
                "$modifier $p ${f.name}"
            }
        }

        val sb = StringBuilder("message Pir${carrierName}Carrier {\n")
        protoFields.forEachIndexed { i, f ->
            sb.append("    $f = ${i + 1}")
            sb.append(";\n")
        }

        if (fields.any { it.proto != null && it.proto.protoPrefix == null}) {
            sb.append("    optional int64 flags = ${protoFields.size + 1} [default = 0];\n")
        }

        sb.append("}\n")

        protoMessages += sb.toString()

        addDeserializerMessage(carrierName, *fields)
    }

    val deserializerMethods = mutableListOf<E>().also { list ->

        list += +"abstract fun deserializeParent(proto: Long): " + IrDeclarationParent
        list += +"abstract fun deserializeOrigin(proto: Int): " + IrDeclarationOrigin
        list += +"abstract fun deserializeAnnotations(proto: List<" + protoIrConstructorCall + ">): List<" + IrConstructorCall + ">"

        listOf(
            bodyProtoType, blockBodyProtoType, expressionBodyProtoType, valueParameterProtoType, valueParameterListProtoType, typeParameterListProtoType, superTypeListProtoType,
            typeProtoType, symbolProtoType, symbolListProtoType, variableProtoType, visibilityProto, modalityProto
        ).forEach { p ->
            val argumentType = if (p.fieldKind == FieldKind.REPEATED) +"List<" + p.protoType + ">" else p.protoType
            val returnType = if (p.fieldKind == FieldKind.REPEATED) +"List<" + p.irType + ">" else p.irType
            list += +"abstract fun deserialize${p.entityName.capitalize()}(proto: "+ argumentType +"): " + returnType
        }
    }

    fun addDeserializerMessage(carrierName: String, vararg fields: Field) {
        val argumentType = import("Pir${carrierName}Carrier", protoPackage)
        val returnType = import("${carrierName}Carrier", carrierPackage)
        val carrierImpl = import("${carrierName}CarrierImpl", carrierPackage)

        deserializerMethods += lines(
            +"fun deserialize${carrierName}Carrier(proto: " + argumentType + "): " + returnType + " {",
            lines(
                +"return " + carrierImpl + "(",
                arrayOf(
                    +"proto.lastModified",
                    +"deserializeParent(proto.parentSymbol)",
                    +"deserializeOrigin(proto.origin)",
                    +"deserializeAnnotations(proto.annotationList)",
                    *fields.map { f ->
                        if (f.proto == null) {
                            +"null"
                        } else {
                            val argument = if (f.proto.protoPrefix != null) {
                                val maybeList = if (f.proto.fieldKind == FieldKind.REPEATED) "List" else ""
                                "proto.${f.name}$maybeList"
                            } else "proto.flags"
                            +"deserialize${f.proto.entityName.capitalize()}($argument)"
                        }
                    }.toTypedArray()
                ).join(separator = ",\n").indent(),
                +")",
            ).indent(),
            +"}",
        )
    }

    // Helpers

    fun baseClasses(name: String, baseClass: String = "Ir$name"): E = lines(
        irDeclaration(baseClass) + "(),",
        +"    PersistentIrDeclarationBase<" + irCarrier("${name}Carrier") + ">,",
        +"    ${name}Carrier",
    )

    fun persistentField(name: String, type: E, initializer: E, lateinit: Boolean = false, modifier: String = "override", isBody: Boolean = false): E = lines(
        +"override var ${name}Field: " + type + "${if (lateinit) "?" else ""} = " + initializer,
        id,
        +"$modifier var $name: " + type,
        lines(
            +"get() = getCarrier().${name}Field${if (lateinit) "!!" else ""}",
            +"set(v) " + block(
                +"if (${if (lateinit) "getCarrier().${name}Field" else name} !== v) " + block(
                    (if (isBody) lines(
                        +"if (v is PersistentIrBodyBase<*>) " + block(
                            +"v.container = this"
                        ),
                        id
                    ) else id) + "setCarrier().${name}Field = v"
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
            *(fields.map { +"var ${it.name}Field: " + it.type + if (it.lateinit) "?" else "" }.toTypedArray()),
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
            +"override var parentField: " + IrDeclarationParent + "?",
            +"override var originField: " + IrDeclarationOrigin,
            +"override var annotationsField: List<" + IrConstructorCall + ">",
            *(fields.map { +"override var ${it.name}Field: " + it.type + if (it.lateinit) "?" else "" }.toTypedArray()),
        ).join(separator = ",\n").indent(),
        +") : ${name}Carrier",
        id,
    )

    fun lines(vararg fn: E): E = fn.join(separator = "\n")

    fun block(vararg fn: E): E = lines(+"{", { indent { lines(*fn)() } }, +"}")

    fun blockSpaced(vararg fn: E): E {
        return block(*(fn.flatMap { listOf(id, it) }.toTypedArray()))
    }

    fun import(name: String, pkg: String, alias: String = name): E = { import("$pkg.$name${ if (alias != name) " as $alias" else ""}").text(alias) }

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

        var result = """
            /*
             * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
             * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
             */

            package $pkg
    """.trimIndent()

        result += "\n\n"
        result += imports.map { "import $it" }.sorted().joinToString(separator = "\n")
        result += "\n\n"
        result += "// Auto-generated by compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/persistentIrGenerator/Main.kt. DO NOT EDIT!\n"
        result += sb.toString()

        result = result.lines().map { if (it.isBlank()) "" else it }.joinToString(separator = "\n")

        return result
    }

    private val prefix = "compiler/ir/ir.tree.persistent/src/org/jetbrains/kotlin/ir/declarations/persistent/"

    fun writeFile(path: String, content: String) {
        File(prefix + path).writeText(content)
    }

    fun updateKotlinIrProto() {
        val file = File("compiler/ir/serialization.common/src/KotlinIr.proto")

        if (!file.exists()) throw IllegalStateException("KotlinIr.proto file not found!")

        val lines = file.readText().lines()

        val start = lines.indexOf("// PIR GENERATOR START")
        if (start < 0) throw IllegalStateException("Couldn't find the '// PIR GENERATOR START' line. Don't know where to write generated messages.")

        val end = lines.indexOf("// PIR GENERATOR END")
        if (end < 0) throw IllegalStateException("Couldn't find the '// PIR GENERATOR END' line. Don't know where to write generated messages.")

        val sb = StringBuilder(lines.subList(0, start + 1).joinToString(separator = "\n"))

        for (m in protoMessages) {
            sb.append("\n").append(m)
        }

        sb.append("\n").append(lines.subList(end, lines.size).joinToString(separator = "\n"))

        file.writeText(sb.toString())
    }

    fun generateCarrierDeserializer() {
        writeFile("../../serialization/IrCarrierDeserializer.kt", renderFile("org.jetbrains.kotlin.ir.persistentIrGenerator") {
            lines(
                id,
                +"internal abstract class IrCarrierDeserializer " + blockSpaced(
                    *deserializerMethods.toTypedArray()
                ),
                id,
            )()
        })
    }
}