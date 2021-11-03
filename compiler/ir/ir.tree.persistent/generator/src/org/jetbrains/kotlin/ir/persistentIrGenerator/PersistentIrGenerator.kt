/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.ir.persistentIrGenerator

import java.io.File

internal interface R {
    fun text(t: String): R

    fun indent(fn: R.() -> Unit): R

    fun import(fqn: String): R
}

internal typealias E = R.() -> R

internal val id: E get() = { this }

internal enum class FieldKind {
    REQUIRED, OPTIONAL, REPEATED
}

internal class Proto(
    val protoPrefix: String?, // null if flag, proto type if not
    val entityName: String,   // what to deserialize
    val protoType: E,         // what type ProtoBuf generates
    val irType: E,            // what Ir class this maps to
    val fieldKind: FieldKind = FieldKind.OPTIONAL,
)

internal class Field(
    val name: String,
    val propType: E,
    val proto: Proto? = null,
    val lateinit: Boolean = false,
    val notEq: String = "!==",
    val propSymbolType: E? = null,
    val symbolToDeclaration: E = id,
    val declarationToSymbol: E = id,
    val storeInCarrierImpl: Boolean = false,
)

internal object PersistentIrGenerator {

    private val protoPackage = "org.jetbrains.kotlin.backend.common.serialization.proto"
    val carrierPackage = "org.jetbrains.kotlin.ir.declarations.persistent.carriers"

    // Imports

    val codedInputStream: E = import("codedInputStream", "org.jetbrains.kotlin.backend.common.serialization")
    val ExtensionRegistryLite: E = import("ExtensionRegistryLite", "org.jetbrains.kotlin.protobuf")

    val ClassDescriptor: E = descriptorType("ClassDescriptor")
    val DeclarationDescriptor: E = descriptorType("DeclarationDescriptor")
    val ClassConstructorDescriptor: E = descriptorType("ClassConstructorDescriptor")
    val DescriptorVisibility = descriptorType("DescriptorVisibility")

    val IrDeclaration = irDeclaration("IrDeclaration")
    val IrDeclarationOrigin = irDeclaration("IrDeclarationOrigin")
    val IrDeclarationParent = irDeclaration("IrDeclarationParent")
    val IrAnonymousInitializer = irDeclaration("IrAnonymousInitializer")
    val IrClass = irDeclaration("IrClass")
    val IrFunction = irDeclaration("IrFunction")
    val IrSimpleFunction = irDeclaration("IrSimpleFunction")
    val IrField = irDeclaration("IrField")
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

    val IrSymbol = irSymbol("IrSymbol")
    val IrPropertySymbol = irSymbol("IrPropertySymbol")
    val IrSimpleFunctionSymbol = irSymbol("IrSimpleFunctionSymbol")
    val IrFunctionSymbol = irSymbol("IrFunctionSymbol")
    val IrFieldSymbol = irSymbol("IrFieldSymbol")
    val IrValueParameterSymbol = irSymbol("IrValueParameterSymbol")
    val IrTypeParameterSymbol = irSymbol("IrTypeParameterSymbol")

    val IdSignature = import("IdSignature", "org.jetbrains.kotlin.ir.util")

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
    val containerSource = +"override val containerSource: " + import(
        "DeserializedContainerSource",
        "org.jetbrains.kotlin.serialization.deserialization.descriptors"
    ) + "?"

    val irFactory = +"override val factory: PersistentIrFactory"

    val initBlock = +"init " + block(
        +"symbol.bind(this)"
    )

    // import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrDeclarationBase.Companion.hashCodeCounter

    val hashCodeValue = "private val hashCodeValue: Int = PersistentIrDeclarationBase.hashCodeCounter++"
    val hashCodeImplementation = "override fun hashCode(): Int = hashCodeValue"
    val equalsImplementation = "override fun equals(other: Any?): Boolean = (this === other)"
    val hashCodeAndEqualsImpl = +"$hashCodeValue\n$hashCodeImplementation\n$equalsImplementation"

    // Proto types

    val protoValueParameterType = import("IrValueParameter", protoPackage, "ProtoIrValueParameter")
    val protoTypeParameterType = import("IrTypeParameter", protoPackage, "ProtoIrTypeParameter")
    val protoVariable = import("IrVariable", protoPackage, "ProtoIrVariable")
    val protoIrConstructorCall = import("IrConstructorCall", protoPackage, "ProtoIrConstructorCall")

    val bodyProto = Proto("int32", "body", +"Int", IrBody)
    val blockBodyProto = Proto("int32", "blockBody", +"Int", IrBlockBody)
    val expressionBodyProto = Proto("int32", "expressionBody", +"Int", IrExpressionBody)
    val valueParameterProto = Proto("int64", "valueParameter", +"Long", IrValueParameterSymbol)
    val valueParameterListProto = Proto("int64", "valueParameter", +"Long", IrValueParameterSymbol, fieldKind = FieldKind.REPEATED)
    val typeParameterListProto = Proto("int64", "typeParameter", +"Long", IrTypeParameterSymbol, fieldKind = FieldKind.REPEATED)
    val superTypeListProto = Proto("int32", "superType", +"Int", IrType, fieldKind = FieldKind.REPEATED)
    val sealedSubclassListProto = Proto("int64", "sealedSubclass", +"Long", IrClassSymbol, fieldKind = FieldKind.REPEATED)
    val typeProto = Proto("int32", "type", +"Int", IrType, fieldKind = FieldKind.REQUIRED)
    val optionalTypeProto = Proto("int32", "type", +"Int", IrType, fieldKind = FieldKind.OPTIONAL)
    val variableProto = Proto("IrVariable", "variable", protoVariable, IrVariable)

    val classProto = Proto("int64", "class", +"Long", IrClassSymbol)
    val propertySymbolProto = Proto("int64", "propertySymbol", +"Long", IrPropertySymbol)
    val simpleFunctionProto = Proto("int64", "simpleFunction", +"Long", IrSimpleFunctionSymbol)
    val simpleFunctionSymbolListProto =
        Proto("int64", "simpleFunctionSymbol", +"Long", IrSimpleFunctionSymbol, fieldKind = FieldKind.REPEATED)
    val propertySymbolListProto =
        Proto("int64", "propertySymbol", +"Long", IrPropertySymbol, fieldKind = FieldKind.REPEATED)

    val fieldProto = Proto("int64", "field", +"Long", IrFieldSymbol)

    val visibilityProto = Proto(null, "visibility", +"Long", DescriptorVisibility)
    val modalityProto = Proto(null, "modality", +"Long", descriptorType("Modality"))
    val inlineClassRepresentationProto = Proto(
        "IrInlineClassRepresentation", "inlineClassRepresentation",
        import("IrInlineClassRepresentation", protoPackage, "ProtoIrInlineClassRepresentation"),
        descriptorType("InlineClassRepresentation") + "<" + import("IrSimpleType", "org.jetbrains.kotlin.ir.types") + ">"
    )

    val isExternalClassProto = Proto(null, "isExternalClass", +"Long", +"Boolean")
    val isExternalFieldProto = Proto(null, "isExternalField", +"Long", +"Boolean")
    val isExternalFunctionProto = Proto(null, "isExternalFunction", +"Long", +"Boolean")
    val isExternalPropertyProto = Proto(null, "isExternalProperty", +"Long", +"Boolean")

    private val allProto = listOf(
        bodyProto,
        blockBodyProto,
        expressionBodyProto,
        valueParameterProto,
        valueParameterListProto,
        typeParameterListProto,
        superTypeListProto,
        sealedSubclassListProto,
        typeProto,
        optionalTypeProto,
        classProto,
        propertySymbolProto,
        simpleFunctionProto,
        simpleFunctionSymbolListProto,
        fieldProto,
        variableProto,
        visibilityProto,
        modalityProto,
        inlineClassRepresentationProto,
        isExternalClassProto,
        isExternalFieldProto,
        isExternalFunctionProto,
        isExternalPropertyProto,
    )

    // Fields
    val signature = +"override var signature: " + IdSignature + "? = factory.currentSignature(this)"

    val lastModified = +"override var lastModified: Int = factory.stageController.currentStage"
    val loweredUpTo = +"override var loweredUpTo: Int = factory.stageController.currentStage"
    val values = +"override var values: Array<" + Carrier + ">? = null"
    val createdOn = +"override val createdOn: Int = factory.stageController.currentStage"

    val parentField = +"override var parentField: " + IrDeclarationParent + "? = null"
    val originField = +"override var originField: " + IrDeclarationOrigin + " = origin"
    val removedOn = +"override var removedOn: Int = Int.MAX_VALUE"
    val annotationsField = +"override var annotationsField: List<" + IrConstructorCall + "> = emptyList()"

    val contextReceiverParametersCount = +"override var contextReceiverParametersCount: Int = 0"

    val commonFields = lines(
        signature,
        id,
        lastModified,
        loweredUpTo,
        values,
        createdOn,
        id,
        parentField,
        originField,
        removedOn,
        annotationsField,
        hashCodeAndEqualsImpl
    )

    val typeParametersField = Field(
        "typeParameters",
        +"List<" + IrTypeParameter + ">",
        typeParameterListProto,
        propSymbolType = +"List<" + IrTypeParameterSymbol + ">",
        symbolToDeclaration = +".map { it.owner }",
        declarationToSymbol = +".map { it.symbol }",
        storeInCarrierImpl = true,
    )

    val valueParametersField = Field(
        "valueParameters",
        +"List<" + IrValueParameter + ">",
        valueParameterListProto,
        propSymbolType = +"List<" + IrValueParameterSymbol + ">",
        symbolToDeclaration = +".map { it.owner }",
        declarationToSymbol = +".map { it.symbol }",
        storeInCarrierImpl = true,
    )

    val dispatchReceiverParameterField = Field(
        "dispatchReceiverParameter",
        IrValueParameter + "?",
        valueParameterProto,
        propSymbolType = IrValueParameterSymbol + "?",
        symbolToDeclaration = +"?.owner",
        declarationToSymbol = +"?.symbol"
    )

    val extensionReceiverParameterField = Field(
        "extensionReceiverParameter",
        IrValueParameter + "?",
        valueParameterProto,
        propSymbolType = IrValueParameterSymbol + "?",
        symbolToDeclaration = +"?.owner",
        declarationToSymbol = +"?.symbol"
    )

    fun Field.toPersistentField(initializer: E, modifier: String = "override") =
        persistentField(
            name,
            propType,
            initializer,
            lateinit,
            modifier,
            notEq = notEq,
            symbolType = propSymbolType,
            declarationToSymbol = declarationToSymbol,
            symbolToDeclaration = symbolToDeclaration,
        )

    fun Field.toBody() = body(propType, lateinit, name)

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
                val modifier = f.proto.fieldKind.toString().lowercase()
                "$modifier $p ${f.name}"
            }
        }

        val sb = StringBuilder("message Pir${carrierName}Carrier {\n")
        protoFields.forEachIndexed { i, f ->
            sb.append("    $f = ${i + 1}")
            sb.append(";\n")
        }

        if (fields.any { it.proto != null && it.proto.protoPrefix == null }) {
            sb.append("    optional int64 flags = ${protoFields.size + 1} [default = 0];\n")
        }

        sb.append("}\n")

        protoMessages += sb.toString()

        addDeserializerMessage(carrierName, *fields)
        addSerializerMessage(carrierName, *fields)
    }

    val deserializerMethods = mutableListOf<E>().also { list ->

        list += +"abstract fun deserializeParentSymbol(proto: Long): " + IrSymbol
        list += +"abstract fun deserializeOrigin(proto: Int): " + IrDeclarationOrigin
        list += +"abstract fun deserializeAnnotation(proto: " + protoIrConstructorCall + "): " + IrConstructorCall

        val seenEntities = mutableSetOf<String>()

        allProto.forEach { p ->
            if (p.entityName !in seenEntities) {
                seenEntities += p.entityName
                list += +"abstract fun deserialize${p.entityName.capitalize()}(proto: " + p.protoType + "): " + p.irType
            }
        }
    }

    fun addDeserializerMessage(carrierName: String, vararg fields: Field) {
        val argumentType = import("Pir${carrierName}Carrier", protoPackage)
        val returnType = import("${carrierName}Carrier", carrierPackage)
        val carrierImpl = import("${carrierName}CarrierImpl", carrierPackage)

        deserializerMethods += lines(
            +"fun deserialize${carrierName}Carrier(bytes: ByteArray): " + returnType + " {",
            lines(
                +"val proto = " + argumentType + ".parseFrom(bytes." + codedInputStream + ", " + ExtensionRegistryLite + ".newInstance())",
                +"return " + carrierImpl + "(",
                arrayOf(
                    +"proto.lastModified",
                    +"if (proto.hasParentSymbol()) deserializeParentSymbol(proto.parentSymbol) else null",
                    +"deserializeOrigin(proto.origin)",
                    +"proto.annotationList.map { deserializeAnnotation(it) }",
                    *fields.map { f ->
                        if (f.proto == null) {
                            +"null"
                        } else {
                            val deserialize = "deserialize${f.proto.entityName.capitalize()}"

                            when {
                                f.proto.fieldKind == FieldKind.REPEATED ->
                                    +"proto.${f.name}List.map { $deserialize(it) }"
                                f.proto.protoPrefix != null && f.proto.fieldKind == FieldKind.OPTIONAL ->
                                    +"if (proto.has${f.name.capitalize()}()) $deserialize(proto.${f.name}) else null"
                                f.proto.protoPrefix == null ->
                                    +"$deserialize(proto.flags)"
                                else ->
                                    +"$deserialize(proto.${f.name})"
                            }
                        }
                    }.toTypedArray()
                ).join(separator = ",\n").indent(),
                +")",
            ).indent(),
            +"}",
        )
    }

    val serializerMethods = mutableListOf<E>().also { list ->

        list += +"abstract fun serializeParentSymbol(value: " + IrSymbol + "): Long"
        list += +"abstract fun serializeOrigin(value: " + IrDeclarationOrigin + "): Int"
        list += +"abstract fun serializeAnnotation(value: " + IrConstructorCall + "): " + protoIrConstructorCall

        val seenEntities = mutableSetOf<String>()

        allProto.forEach { p ->
            if (p.entityName !in seenEntities) {
                seenEntities += p.entityName
                list += +"abstract fun serialize${p.entityName.capitalize()}(value: " + p.irType + "): " + p.protoType
            }
        }
    }


    fun addSerializerMessage(carrierName: String, vararg fields: Field) {
        val argumentType = import("${carrierName}Carrier", carrierPackage)
        val returnType = import("Pir${carrierName}Carrier", protoPackage)

        var flagsHandled = false

        serializerMethods += lines(
            +"fun serialize${carrierName}Carrier(carrier: " + argumentType + "): ByteArray {",
            lines(
                +"val proto = " + returnType + ".newBuilder()",
                +"proto.setLastModified(carrier.lastModified)",
                +"carrier.parentSymbolField?.let { proto.setParentSymbol(serializeParentSymbol(it)) }",
                +"proto.setOrigin(serializeOrigin(carrier.originField))",
                +"proto.addAllAnnotation(carrier.annotationsField.map { serializeAnnotation(it) })",
                *(fields.mapNotNull { f ->
                    f.proto?.let { p ->
                        if (p.protoPrefix != null) {
                            val action = "proto." + (if (p.fieldKind == FieldKind.REPEATED) "addAll" else "set") + f.name.capitalize()
                            val serializationFun = "serialize${f.proto.entityName.capitalize()}"
                            val argument = "carrier.${f.name}${if (f.propSymbolType != null) "Symbol" else ""}Field"

                            when {
                                p.fieldKind == FieldKind.OPTIONAL ->
                                    +"$argument?.let { $action($serializationFun(it)) }"
                                p.fieldKind == FieldKind.REPEATED ->
                                    +"$action($argument.map { $serializationFun(it) })"
                                else ->
                                    +"$action($serializationFun($argument))"
                            }
                        } else {
                            // It's a flag
                            if (!flagsHandled) {
                                flagsHandled = true
                                val flags = fields.filter { it.proto?.protoPrefix == null }

                                val calls = flags.map { f -> "serialize${f.proto!!.entityName.capitalize()}(carrier.${f.name}Field)" }

                                +"proto.setFlags(${calls.joinToString(separator = " or ")})"
                            } else null
                        }
                    }
                }).toTypedArray(),
                +"return proto.build().toByteArray()",
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

    fun persistentField(
        name: String,
        type: E,
        initializer: E,
        lateinit: Boolean = false,
        modifier: String = "override",
        isBody: Boolean = false,
        notEq: String = "!==",
        symbolType: E? = null,
        declarationToSymbol: E = id,
        symbolToDeclaration: E = id,
    ): E {
        val result = mutableListOf(+"override var ${name}Field: " + type + "${if (lateinit) "?" else ""} = " + initializer, id)
        if (symbolType != null) {
            result += +"override var ${name}SymbolField: " + symbolType + if (lateinit) "?" else ""
            result += lines(
                +"get() = ${name}Field" + (if (lateinit) "?" else "") + declarationToSymbol,
                +"set(v) " + block(
                    +"${name}Field = v" + (if (lateinit) "?" else "") + symbolToDeclaration
                )
            ).indent()
            result += id
        }

        result += +"$modifier var $name: " + type
        result += lines(
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

        return lines(*result.toTypedArray())
    }

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
            *(fields.flatMap {
                var result = listOf(+"val ${it.name}Field: " + it.propType + if (it.lateinit) "?" else "")

                if (it.propSymbolType != null) {
                    result += +"val ${it.name}SymbolField: " + it.propSymbolType + if (it.lateinit) "?" else ""
                }

                result
            }.toTypedArray()),
            id,
            +"override fun clone(): ${name}Carrier " + block(
                +"return ${name}CarrierImpl(",
                arrayOf(
                    +"lastModified",
                    +"parentSymbolField",
                    +"originField",
                    +"annotationsField",
                    *(fields.map { +"${it.name}${if (it.propSymbolType != null) "Symbol" else ""}Field" }.toTypedArray())
                ).join(separator = ",\n").indent(),
                +")",
            )
        ),
        id,
        +"internal class ${name}CarrierImpl(",
        arrayOf(
            +"override val lastModified: Int",
            +"override val parentSymbolField: " + IrSymbol + "?",
            +"override val originField: " + IrDeclarationOrigin,
            +"override val annotationsField: List<" + IrConstructorCall + ">",
            *(fields.map {
                if (it.propSymbolType != null) {
                    +"override val ${it.name}SymbolField: " + it.propSymbolType + if (it.lateinit) "?" else ""
                } else {
                    +"override val ${it.name}Field: " + it.propType + if (it.lateinit) "?" else ""
                }
            }.toTypedArray()),
        ).join(separator = ",\n").indent(),
        +") : ${name}Carrier" + if (fields.all { it.propSymbolType == null }) id else + " " + blockSpaced(
            *(fields.mapNotNull {
                it.propSymbolType?.let { _ ->
                    if (it.storeInCarrierImpl) {
                        +"override val ${it.name}Field: " + it.propType + " by lazy { ${it.name}SymbolField" + it.symbolToDeclaration + " }"
                    } else {
                        lines(
                            +"override val ${it.name}Field: " + it.propType + if (it.lateinit) "?" else "",
                            lines(
                                +"get() = ${it.name}SymbolField" + (if (it.lateinit) "?" else "") + it.symbolToDeclaration,
                            ).indent()
                        )
                    }
                }
            }).toTypedArray()
        ),
        id,
    )

    fun lines(vararg fn: E): E = fn.join(separator = "\n")

    fun block(vararg fn: E): E = lines(+"{", { indent { lines(*fn)() } }, +"}")

    fun blockSpaced(vararg fn: E): E {
        return block(*(fn.flatMap { listOf(id, it) }.toTypedArray()))
    }

    fun import(name: String, pkg: String, alias: String = name): E =
        { import("$pkg.$name${if (alias != name) " as $alias" else ""}").text(alias) }

    fun descriptorType(name: String): E = import(name, "org.jetbrains.kotlin.descriptors")

    fun irDeclaration(name: String): E = import(name, "org.jetbrains.kotlin.ir.declarations")

    fun irExpression(name: String): E = import(name, "org.jetbrains.kotlin.ir.expressions")

    fun irCarrier(name: String): E = import(name, "org.jetbrains.kotlin.ir.declarations.persistent.carriers")

    fun irSymbol(name: String): E = import(name, "org.jetbrains.kotlin.ir.symbols")

    infix operator fun E.plus(e: E?): E = { this@plus(); e.safe()() }

    infix operator fun E.plus(e: String): E = this + (+e)

    operator fun String.unaryPlus(): E = { text(this@unaryPlus) }

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

    fun generateKotlinPirCarriersProto() {
        val file = File("compiler/ir/serialization.common/src/KotlinPirCarriers.proto")

        val sb = StringBuilder("""
            syntax = "proto2";
            package org.jetbrains.kotlin.backend.common.serialization.proto;

            option java_multiple_files = true;
            option java_outer_classname = "KotlinIr";
            option optimize_for = LITE_RUNTIME;

            import "compiler/ir/serialization.common/src/KotlinIr.proto";
            
            // Auto-generated by compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/persistentIrGenerator/Main.kt. DO NOT EDIT!
            
        """.trimIndent())

        for (m in protoMessages) {
            sb.append("\n").append(m)
        }

        file.writeText(sb.toString())
    }

    fun generateCarrierDeserializer() {
        writeFile("../../serialization/IrCarrierDeserializer.kt", renderFile("org.jetbrains.kotlin.ir.serialization") {
            lines(
                id,
                +"internal abstract class IrCarrierDeserializer " + blockSpaced(
                    *deserializerMethods.toTypedArray()
                ),
                id,
            )()
        })
    }

    fun generateCarrierSerializer() {
        writeFile("../../serialization/IrCarrierSerializer.kt", renderFile("org.jetbrains.kotlin.ir.serialization") {
            lines(
                id,
                +"internal abstract class IrCarrierSerializer " + blockSpaced(
                    *serializerMethods.toTypedArray()
                ),
                id,
            )()
        })
    }
}

private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }