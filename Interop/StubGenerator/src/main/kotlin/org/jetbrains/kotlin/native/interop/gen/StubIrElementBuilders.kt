/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.*

internal class MacroConstantStubBuilder(
        override val context: StubsBuildingContext,
        private val constant: ConstantDef
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val kotlinName = constant.name
        val declaration = when (constant) {
            is IntegerConstantDef -> {
                val literal = context.tryCreateIntegralStub(constant.type, constant.value) ?: return emptyList()
                val kotlinType = context.mirror(constant.type).argType.toStubIrType()
                when (context.platform) {
                    KotlinPlatform.NATIVE -> PropertyStub(kotlinName, kotlinType, PropertyStub.Kind.Constant(literal))
                    // No reason to make it const val with backing field on Kotlin/JVM yet:
                    KotlinPlatform.JVM -> {
                        val getter = PropertyAccessor.Getter.SimpleGetter(constant = literal)
                        PropertyStub(kotlinName, kotlinType, PropertyStub.Kind.Val(getter))
                    }
                }
            }
            is FloatingConstantDef -> {
                val literal = context.tryCreateDoubleStub(constant.type, constant.value) ?: return emptyList()
                val kotlinType = context.mirror(constant.type).argType.toStubIrType()
                val getter = PropertyAccessor.Getter.SimpleGetter(constant = literal)
                PropertyStub(kotlinName, kotlinType, PropertyStub.Kind.Val(getter))
            }
            is StringConstantDef -> {
                val literal = StringConstantStub(constant.value.quoteAsKotlinLiteral())
                val kotlinType = KotlinTypes.string.toStubIrType()
                val getter = PropertyAccessor.Getter.SimpleGetter(constant = literal)
                PropertyStub(kotlinName, kotlinType, PropertyStub.Kind.Val(getter))
            }
            else -> return emptyList()
        }
        return listOf(declaration)
    }
}

internal class StructStubBuilder(
        override val context: StubsBuildingContext,
        private val decl: StructDecl
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val platform = context.platform
        val def = decl.def ?: return generateForwardStruct(decl)

        val structAnnotation: AnnotationStub? = if (platform == KotlinPlatform.JVM) {
            if (def.kind == StructDef.Kind.STRUCT && def.fieldsHaveDefaultAlignment()) {
                AnnotationStub.CNaturalStruct(def.members)
            } else {
                null
            }
        } else {
            tryRenderStructOrUnion(def)?.let {
                AnnotationStub.CStruct(it)
            }
        }
        val classifier = context.getKotlinClassForPointed(decl)

        val fields: List<PropertyStub?> = def.fields.map { field ->
            try {
                assert(field.name.isNotEmpty())
                assert(field.offset % 8 == 0L)
                val offset = field.offset / 8
                val fieldRefType = context.mirror(field.type)
                val unwrappedFieldType = field.type.unwrapTypedefs()
                if (unwrappedFieldType is ArrayType) {
                    val type = (fieldRefType as TypeMirror.ByValue).valueType
                    val annotations = if (platform == KotlinPlatform.JVM) {
                        val length = getArrayLength(unwrappedFieldType)
                        // TODO: @CLength should probably be used on types instead of properties.
                        listOf(AnnotationStub.CLength(length))
                    } else {
                        emptyList()
                    }
                    val kind = PropertyStub.Kind.Val(PropertyAccessor.Getter.ArrayMemberAt(offset))
                    // TODO: Should receiver be added?
                    PropertyStub(field.name, type.toStubIrType(), kind, annotations = annotations)
                } else {
                    val pointedType = fieldRefType.pointedType.toStubIrType()
                    val pointedTypeArgument = TypeArgumentStub(pointedType)
                    if (fieldRefType is TypeMirror.ByValue) {
                        val kind = PropertyStub.Kind.Var(
                                PropertyAccessor.Getter.MemberAt(offset, typeArguments = listOf(pointedTypeArgument), hasValueAccessor = true),
                                PropertyAccessor.Setter.MemberAt(offset, typeArguments = listOf(pointedTypeArgument))
                        )
                        PropertyStub(field.name, fieldRefType.argType.toStubIrType(), kind)
                    } else {
                        val kind = PropertyStub.Kind.Val(PropertyAccessor.Getter.MemberAt(offset, hasValueAccessor = false))
                        PropertyStub(field.name, pointedType, kind)
                    }
                }
            } catch (e: Throwable) {
                null
            }
        }

        val bitFields: List<PropertyStub> = def.bitFields.map { field ->
            val typeMirror = context.mirror(field.type)
            val typeInfo = typeMirror.info
            val kotlinType = typeMirror.argType
            val signed = field.type.isIntegerTypeSigned()
            val readBits = PropertyAccessor.Getter.ReadBits(field.offset, field.size, signed)
            val writeBits = PropertyAccessor.Setter.WriteBits(field.offset, field.size)
            context.bridgeComponentsBuilder.getterToBridgeInfo[readBits] = BridgeGenerationInfo("", typeInfo)
            context.bridgeComponentsBuilder.setterToBridgeInfo[writeBits] = BridgeGenerationInfo("", typeInfo)
            val kind = PropertyStub.Kind.Var(readBits, writeBits)
            PropertyStub(field.name, kotlinType.toStubIrType(), kind)
        }

        val superClass = context.platform.getRuntimeType("CStructVar")
        require(superClass is ClassifierStubType)
        val rawPtrConstructorParam = ConstructorParameterStub("rawPtr", context.platform.getRuntimeType("NativePtr"))
        val superClassInit = SuperClassInit(superClass, listOf(GetConstructorParameter(rawPtrConstructorParam)))

        val companionSuper = superClass.nested("Type")
        val typeSize = listOf(IntegralConstantStub(def.size, 4, true), IntegralConstantStub(def.align.toLong(), 4, true))
        val companionSuperInit = SuperClassInit(companionSuper, typeSize)
        val companion = ClassStub.Companion(companionSuperInit)

        return listOf(ClassStub.Simple(
                classifier,
                origin = StubOrigin.Struct(decl),
                properties = fields.filterNotNull() + if (platform == KotlinPlatform.NATIVE) bitFields else emptyList(),
                functions = emptyList(),
                modality = ClassStubModality.NONE,
                annotations = listOfNotNull(structAnnotation),
                superClassInit = superClassInit,
                constructorParameters = listOf(rawPtrConstructorParam),
                companion = companion
        ))
    }

    private fun getArrayLength(type: ArrayType): Long {
        val unwrappedElementType = type.elemType.unwrapTypedefs()
        val elementLength = if (unwrappedElementType is ArrayType) {
            getArrayLength(unwrappedElementType)
        } else {
            1L
        }

        val elementCount = when (type) {
            is ConstArrayType -> type.length
            is IncompleteArrayType -> 0L
            else -> TODO(type.toString())
        }

        return elementLength * elementCount
    }

    private tailrec fun Type.isIntegerTypeSigned(): Boolean = when (this) {
        is IntegerType -> this.isSigned
        is BoolType -> false
        is EnumType -> this.def.baseType.isIntegerTypeSigned()
        is Typedef -> this.def.aliased.isIntegerTypeSigned()
        else -> error(this)
    }

    /**
     * Produces to [out] the definition of Kotlin class representing the reference to given forward (incomplete) struct.
     */
    private fun generateForwardStruct(s: StructDecl): List<StubIrElement> = when (context.platform) {
        KotlinPlatform.JVM -> {
            val classifier = context.getKotlinClassForPointed(s)
            val superClass = context.platform.getRuntimeType("COpaque")
            val rawPtrConstructorParam = ConstructorParameterStub("rawPtr", context.platform.getRuntimeType("NativePtr"))
            val superClassInit = SuperClassInit(superClass, listOf(GetConstructorParameter(rawPtrConstructorParam)))
            val origin = StubOrigin.Struct(s)
            listOf(ClassStub.Simple(classifier, ClassStubModality.NONE, listOf(rawPtrConstructorParam), superClassInit, origin = origin))
        }
        KotlinPlatform.NATIVE -> emptyList()
    }
}

internal class EnumStubBuilder(
        override val context: StubsBuildingContext,
        private val enumDef: EnumDef
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        if (!context.isStrictEnum(enumDef)) {
            return generateEnumAsConstants(enumDef)
        }
        val baseTypeMirror = context.mirror(enumDef.baseType)
        val baseType = baseTypeMirror.argType.toStubIrType()

        val clazz = (context.mirror(EnumType(enumDef)) as TypeMirror.ByValue).valueType.classifier
        val qualifier = ConstructorParameterStub.Qualifier.VAL(overrides = true)
        val valueParamStub = ConstructorParameterStub("value", baseType, qualifier)

        val canonicalsByValue = enumDef.constants
                .groupingBy { it.value }
                .reduce { _, accumulator, element ->
                    if (element.isMoreCanonicalThan(accumulator)) {
                        element
                    } else {
                        accumulator
                    }
                }
        val (canonicalConstants, aliasConstants) = enumDef.constants.partition { canonicalsByValue[it.value] == it }

        val canonicalEntries = canonicalConstants.map { constant ->
            val literal = context.tryCreateIntegralStub(enumDef.baseType, constant.value)
                    ?: error("Cannot create enum value ${constant.value} of type ${enumDef.baseType}")
            val aliases = aliasConstants.filter { it.value == constant.value }.map { EnumEntryStub.Alias(it.name) }
            EnumEntryStub(constant.name, literal, aliases)
        }

        val enum = ClassStub.Enum(clazz, canonicalEntries,
                origin = StubOrigin.Enum(enumDef),
                constructorParameters = listOf(valueParamStub),
                interfaces = listOf(context.platform.getRuntimeType("CEnum"))
        )
        context.bridgeComponentsBuilder.enumToTypeMirror[enum] = baseTypeMirror

        return listOf(enum)
    }


    private fun EnumConstant.isMoreCanonicalThan(other: EnumConstant): Boolean = with(other.name.toLowerCase()) {
        contains("min") || contains("max") ||
                contains("first") || contains("last") ||
                contains("begin") || contains("end")
    }

    /**
     * Produces to [out] the Kotlin definitions for given enum which shouldn't be represented as Kotlin enum.
     */
    private fun generateEnumAsConstants(e: EnumDef): List<StubIrElement> {
        // TODO: if this enum defines e.g. a type of struct field, then it should be generated inside the struct class
        //  to prevent name clashing

        val entries = mutableListOf<PropertyStub>()
        val typealiases = mutableListOf<TypealiasStub>()

        val constants = e.constants.filter {
            // Macro "overrides" the original enum constant.
            it.name !in context.macroConstantsByName
        }

        val kotlinType: KotlinType

        val baseKotlinType = context.mirror(e.baseType).argType
        val meta = if (e.isAnonymous) {
            kotlinType = baseKotlinType
            StubContainerMeta(textAtStart = if (constants.isNotEmpty()) "// ${e.spelling}:" else "")
        } else {
            val typeMirror = context.mirror(EnumType(e))
            if (typeMirror !is TypeMirror.ByValue) {
                error("unexpected enum type mirror: $typeMirror")
            }

            val varTypeName = typeMirror.info.constructPointedType(typeMirror.valueType)
            val varTypeClassifier = typeMirror.pointedType.classifier
            val valueTypeClassifier = typeMirror.valueType.classifier
            typealiases += TypealiasStub(varTypeClassifier, varTypeName.toStubIrType())
            typealiases += TypealiasStub(valueTypeClassifier, baseKotlinType.toStubIrType())

            kotlinType = typeMirror.valueType
            StubContainerMeta()
        }

        for (constant in constants) {
            val literal = context.tryCreateIntegralStub(e.baseType, constant.value) ?: continue
            val getter = PropertyAccessor.Getter.SimpleGetter(constant = literal)
            val kind = PropertyStub.Kind.Val(getter)
            entries += PropertyStub(
                    constant.name,
                    kotlinType.toStubIrType(),
                    kind,
                    MemberStubModality.FINAL,
                    null
            )
        }
        val container = SimpleStubContainer(
                meta,
                properties = entries.toList(),
                typealiases = typealiases.toList()
        )
        return listOf(container)
    }
}

internal class FunctionStubBuilder(
        override val context: StubsBuildingContext,
        private val func: FunctionDecl
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val platform = context.platform
        val parameters = mutableListOf<FunctionParameterStub>()

        func.parameters.forEachIndexed { index, parameter ->
            val parameterName = parameter.name.let {
                if (it == null || it.isEmpty()) {
                    "arg$index"
                } else {
                    it
                }
            }

            val representAsValuesRef = representCFunctionParameterAsValuesRef(parameter.type)
            val origin = StubOrigin.FunctionParameter(parameter)
            parameters += when {
                representCFunctionParameterAsString(func, parameter.type) -> {
                    val annotations = when (platform) {
                        KotlinPlatform.JVM -> emptyList()
                        KotlinPlatform.NATIVE -> listOf(AnnotationStub.CCall.CString)
                    }
                    val type = KotlinTypes.string.makeNullable().toStubIrType()
                    val functionParameterStub = FunctionParameterStub(parameterName, type, annotations, origin = origin)
                    context.bridgeComponentsBuilder.cStringParameters += functionParameterStub
                    functionParameterStub
                }
                representCFunctionParameterAsWString(func, parameter.type) -> {
                    val annotations = when (platform) {
                        KotlinPlatform.JVM -> emptyList()
                        KotlinPlatform.NATIVE -> listOf(AnnotationStub.CCall.WCString)
                    }
                    val type = KotlinTypes.string.makeNullable().toStubIrType()
                    val functionParameterStub = FunctionParameterStub(parameterName, type, annotations, origin = origin)
                    context.bridgeComponentsBuilder.wCStringParameters += functionParameterStub
                    functionParameterStub
                }
                representAsValuesRef != null -> {
                    FunctionParameterStub(parameterName, representAsValuesRef.toStubIrType(), origin = origin)
                }
                else -> {
                    val mirror = context.mirror(parameter.type)
                    val type = mirror.argType.toStubIrType()
                    FunctionParameterStub(parameterName, type, origin = origin)
                }
            }
        }

        val returnType = if (func.returnsVoid()) {
            KotlinTypes.unit
        } else {
            context.mirror(func.returnType).argType
        }.toStubIrType()


        val annotations: List<AnnotationStub>
        val mustBeExternal: Boolean
        if (platform == KotlinPlatform.JVM) {
            annotations = emptyList()
            mustBeExternal = false
        } else {
            if (func.isVararg) {
                val type = KotlinTypes.any.makeNullable().toStubIrType()
                parameters += FunctionParameterStub("variadicArguments", type, isVararg = true)
            }
            annotations = listOf(AnnotationStub.CCall.Symbol("${context.generateNextUniqueId("knifunptr_")}_${func.name}"))
            mustBeExternal = true
        }
        val functionStub = FunctionStub(
                func.name,
                returnType,
                parameters.toList(),
                StubOrigin.Function(func),
                annotations,
                mustBeExternal,
                null,
                MemberStubModality.FINAL
        )
        return listOf(functionStub)
    }


    private fun FunctionDecl.returnsVoid(): Boolean = this.returnType.unwrapTypedefs() is VoidType

    private fun representCFunctionParameterAsValuesRef(type: Type): KotlinType? {
        val pointeeType = when (type) {
            is PointerType -> type.pointeeType
            is ArrayType -> type.elemType
            else -> return null
        }

        val unwrappedPointeeType = pointeeType.unwrapTypedefs()

        if (unwrappedPointeeType is VoidType) {
            // Represent `void*` as `CValuesRef<*>?`:
            return KotlinTypes.cValuesRef.typeWith(StarProjection).makeNullable()
        }

        if (unwrappedPointeeType is FunctionType) {
            // Don't represent function pointer as `CValuesRef<T>?` currently:
            return null
        }

        if (unwrappedPointeeType is ArrayType) {
            return representCFunctionParameterAsValuesRef(pointeeType)
        }


        return KotlinTypes.cValuesRef.typeWith(context.mirror(pointeeType).pointedType).makeNullable()
    }


    private val platformWStringTypes = setOf("LPCWSTR")

    private val noStringConversion: Set<String>
        get() = context.configuration.noStringConversion

    private fun Type.isAliasOf(names: Set<String>): Boolean {
        var type = this
        while (type is Typedef) {
            if (names.contains(type.def.name)) return true
            type = type.def.aliased
        }
        return false
    }

    private fun representCFunctionParameterAsString(function: FunctionDecl, type: Type): Boolean {
        val unwrappedType = type.unwrapTypedefs()
        return unwrappedType is PointerType && unwrappedType.pointeeIsConst &&
                unwrappedType.pointeeType.unwrapTypedefs() == CharType &&
                !noStringConversion.contains(function.name)
    }

    // We take this approach as generic 'const short*' shall not be used as String.
    private fun representCFunctionParameterAsWString(function: FunctionDecl, type: Type) = type.isAliasOf(platformWStringTypes)
            && !noStringConversion.contains(function.name)
}

internal class GlobalStubBuilder(
        override val context: StubsBuildingContext,
        private val global: GlobalDecl
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val mirror = context.mirror(global.type)
        val unwrappedType = global.type.unwrapTypedefs()

        val kotlinType: KotlinType
        val kind: PropertyStub.Kind
        if (unwrappedType is ArrayType) {
            kotlinType = (mirror as TypeMirror.ByValue).valueType
            val getter = PropertyAccessor.Getter.SimpleGetter()
            val extra = BridgeGenerationInfo(global.name, mirror.info)
            context.bridgeComponentsBuilder.arrayGetterBridgeInfo[getter] = extra
            kind = PropertyStub.Kind.Val(getter)
        } else {
            when (mirror) {
                is TypeMirror.ByValue -> {
                    kotlinType = mirror.argType
                    val getter = when (context.platform) {
                        KotlinPlatform.JVM -> {
                            PropertyAccessor.Getter.SimpleGetter().also {
                                val getterExtra = BridgeGenerationInfo(global.name, mirror.info)
                                context.bridgeComponentsBuilder.getterToBridgeInfo[it] = getterExtra
                            }
                        }
                        KotlinPlatform.NATIVE -> {
                            val cCallAnnotation = AnnotationStub.CCall.Symbol("${context.generateNextUniqueId("knifunptr_")}_${global.name}_getter")
                            PropertyAccessor.Getter.ExternalGetter(listOf(cCallAnnotation)).also {
                                context.wrapperComponentsBuilder.getterToWrapperInfo[it] = WrapperGenerationInfo(global)
                            }
                        }
                    }
                    kind = if (global.isConst) {
                        PropertyStub.Kind.Val(getter)
                    } else {
                        val setter = when (context.platform) {
                            KotlinPlatform.JVM -> {
                                PropertyAccessor.Setter.SimpleSetter().also {
                                    val setterExtra = BridgeGenerationInfo(global.name, mirror.info)
                                    context.bridgeComponentsBuilder.setterToBridgeInfo[it] = setterExtra
                                }
                            }
                            KotlinPlatform.NATIVE -> {
                                val cCallAnnotation = AnnotationStub.CCall.Symbol("${context.generateNextUniqueId("knifunptr_")}_${global.name}_setter")
                                PropertyAccessor.Setter.ExternalSetter(listOf(cCallAnnotation)).also {
                                    context.wrapperComponentsBuilder.setterToWrapperInfo[it] = WrapperGenerationInfo(global)
                                }
                            }
                        }
                        PropertyStub.Kind.Var(getter, setter)
                    }
                }
                is TypeMirror.ByRef -> {
                    kotlinType = mirror.pointedType
                    val getter = PropertyAccessor.Getter.InterpretPointed(global.name, kotlinType.toStubIrType())
                    kind = PropertyStub.Kind.Val(getter)
                }
            }
        }
        return listOf(PropertyStub(global.name, kotlinType.toStubIrType(), kind))
    }
}

internal class TypedefStubBuilder(
        override val context: StubsBuildingContext,
        private val typedefDef: TypedefDef
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val mirror = context.mirror(Typedef(typedefDef))
        val baseMirror = context.mirror(typedefDef.aliased)

        val varType = mirror.pointedType.classifier
        return when (baseMirror) {
            is TypeMirror.ByValue -> {
                val valueType = (mirror as TypeMirror.ByValue).valueType
                val varTypeAliasee = mirror.info.constructPointedType(valueType)
                val valueTypeAliasee = baseMirror.valueType
                listOf(
                        TypealiasStub(varType, varTypeAliasee.toStubIrType()),
                        TypealiasStub(valueType.classifier, valueTypeAliasee.toStubIrType())
                )
            }
            is TypeMirror.ByRef -> {
                val varTypeAliasee = baseMirror.pointedType
                listOf(TypealiasStub(varType, varTypeAliasee.toStubIrType()))
            }
        }
    }
}