/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

interface DeclarationMapper {
    fun getKotlinClassForPointed(structDecl: StructDecl): Classifier
    fun isMappedToStrict(enumDef: EnumDef): Boolean
    fun getKotlinNameForValue(enumDef: EnumDef): String
    fun getPackageFor(declaration: TypeDeclaration): String
}

fun DeclarationMapper.getKotlinClassFor(
        objCClassOrProtocol: ObjCClassOrProtocol,
        isMeta: Boolean = false
): Classifier {
    val pkg = if (objCClassOrProtocol.isForwardDeclaration) {
        when (objCClassOrProtocol) {
            is ObjCClass -> "objcnames.classes"
            is ObjCProtocol -> "objcnames.protocols"
        }
    } else {
        this.getPackageFor(objCClassOrProtocol)
    }
    val className = objCClassOrProtocol.kotlinClassName(isMeta)
    return Classifier.topLevel(pkg, className)
}

val PrimitiveType.kotlinType: KotlinClassifierType
    get() = when (this) {
        is CharType -> KotlinTypes.byte

        is BoolType -> KotlinTypes.boolean

    // TODO: C primitive types should probably be generated as type aliases for Kotlin types.
        is IntegerType -> when (this.size) {
            1 -> KotlinTypes.byte
            2 -> KotlinTypes.short
            4 -> KotlinTypes.int
            8 -> KotlinTypes.long
            else -> TODO(this.toString())
        }

        is FloatingType -> when (this.size) {
            4 -> KotlinTypes.float
            8 -> KotlinTypes.double
            else -> TODO(this.toString())
        }

        else -> throw NotImplementedError()
    }

private val PrimitiveType.bridgedType: BridgedType
    get() {
        val kotlinType = this.kotlinType
        return BridgedType.values().single {
            it.kotlinType == kotlinType
        }
    }

private val ObjCPointer.isNullable: Boolean
    get() = this.nullability != ObjCPointer.Nullability.NonNull

/**
 * Describes the Kotlin types used to represent some C type.
 */
sealed class TypeMirror(val pointedType: KotlinClassifierType, val info: TypeInfo) {
    /**
     * Type to be used in bindings for argument or return value.
     */
    abstract val argType: KotlinType

    /**
     * Mirror for C type to be represented in Kotlin as by-value type.
     */
    class ByValue(
            pointedType: KotlinClassifierType,
            info: TypeInfo,
            val valueType: KotlinType,
            val nullable: Boolean = (info is TypeInfo.Pointer)
    ) : TypeMirror(pointedType, info) {

        override val argType: KotlinType
            get() = valueType.makeNullableAsSpecified(nullable)
    }

    /**
     * Mirror for C type to be represented in Kotlin as by-ref type.
     */
    class ByRef(pointedType: KotlinClassifierType, info: TypeInfo) : TypeMirror(pointedType, info) {
        override val argType: KotlinType get() = KotlinTypes.cValue.typeWith(pointedType)
    }
}

/**
 * Describes various type conversions for [TypeMirror].
 */
sealed class TypeInfo {
    /**
     * The conversion from [TypeMirror.argType] to [bridgedType].
     */
    abstract fun argToBridged(expr: KotlinExpression): KotlinExpression

    /**
     * The conversion from [bridgedType] to [TypeMirror.argType].
     */
    abstract fun argFromBridged(
            expr: KotlinExpression,
            scope: KotlinScope,
            nativeBacked: NativeBacked
    ): KotlinExpression

    abstract val bridgedType: BridgedType

    open fun cFromBridged(
            expr: NativeExpression,
            scope: NativeScope,
            nativeBacked: NativeBacked
    ): NativeExpression = expr

    open fun cToBridged(expr: NativeExpression): NativeExpression = expr

    /**
     * If this info is for [TypeMirror.ByValue], then this method describes how to
     * construct pointed-type from value type.
     */
    abstract fun constructPointedType(valueType: KotlinType): KotlinClassifierType

    class Primitive(override val bridgedType: BridgedType, val varClass: Classifier) : TypeInfo() {

        override fun argToBridged(expr: KotlinExpression) = expr
        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) = expr

        override fun constructPointedType(valueType: KotlinType) = varClass.typeWith(valueType)
    }

    class Boolean : TypeInfo() {
        override fun argToBridged(expr: KotlinExpression) = "$expr.toByte()"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                "$expr.toBoolean()"

        override val bridgedType: BridgedType get() = BridgedType.BYTE

        override fun cFromBridged(expr: NativeExpression, scope: NativeScope, nativeBacked: NativeBacked) =
                "($expr) ? 1 : 0"

        override fun cToBridged(expr: NativeExpression) = "($expr) ? 1 : 0"

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.booleanVarOf.typeWith(valueType)
    }

    class Enum(val clazz: Classifier, override val bridgedType: BridgedType) : TypeInfo() {
        override fun argToBridged(expr: KotlinExpression) = "$expr.value"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                scope.reference(clazz) + ".byValue($expr)"

        override fun constructPointedType(valueType: KotlinType) =
                clazz.nested("Var").type // TODO: improve

    }

    class Pointer(val pointee: KotlinType) : TypeInfo() {
        override fun argToBridged(expr: String) = "$expr.rawValue"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                "interpretCPointer<${pointee.render(scope)}>($expr)"

        override val bridgedType: BridgedType
            get() = BridgedType.NATIVE_PTR

        override fun cFromBridged(expr: NativeExpression, scope: NativeScope, nativeBacked: NativeBacked) =
                "(void*)$expr" // Note: required for JVM

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.cPointerVarOf.typeWith(valueType)
    }

    class ObjCPointerInfo(val kotlinType: KotlinType, val type: ObjCPointer) : TypeInfo() {
        override fun argToBridged(expr: String) = "$expr.rawPtr()"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                "interpretObjCPointerOrNull<${kotlinType.render(scope)}>($expr)" +
                        if (type.isNullable) "" else "!!"

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.objCObjectVar.typeWith(valueType)
    }

    class NSString(val type: ObjCPointer) : TypeInfo() {
        override fun argToBridged(expr: String) = "CreateNSStringFromKString($expr)"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                "CreateKStringFromNSString($expr)" + if (type.isNullable) "" else "!!"

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        override fun constructPointedType(valueType: KotlinType): KotlinClassifierType {
            return KotlinTypes.objCStringVarOf.typeWith(valueType)
        }
    }

    class ObjCBlockPointerInfo(val kotlinType: KotlinFunctionType, val type: ObjCBlockPointer) : TypeInfo() {

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        // When passing Kotlin function as block pointer from Kotlin to native,
        // it first gets wrapped by a holder in [argToBridged],
        // and then converted to block in [cFromBridged].

        override fun argToBridged(expr: KotlinExpression): KotlinExpression = "createKotlinObjectHolder($expr)"

        override fun cFromBridged(
                expr: NativeExpression,
                scope: NativeScope,
                nativeBacked: NativeBacked
        ): NativeExpression {
            val mappingBridgeGenerator = scope.mappingBridgeGenerator

            val blockParameters = type.parameterTypes.mapIndexed { index, it ->
                "p$index" to it.getStringRepresentation()
            }.joinToString { "${it.second} ${it.first}" }

            val blockReturnType = type.returnType.getStringRepresentation()

            val kniFunction = "kniFunction"

            val codeBuilder = NativeCodeBuilder(scope)

            return buildString {
                append("({ ") // Statement expression begins.
                append("id $kniFunction = $expr; ") // Note: it gets captured below.
                append("($kniFunction == nil) ? nil : ")
                append("(id)") // Cast the block to `id`.
                append("^$blockReturnType($blockParameters) {") // Block begins.

                // As block body, generate the code which simply bridges to Kotlin and calls the Kotlin function:
                mappingBridgeGenerator.nativeToKotlin(
                        codeBuilder,
                        nativeBacked,
                        type.returnType,
                        type.parameterTypes.mapIndexed { index, it ->
                            TypedNativeValue(it, "p$index")
                        } + TypedNativeValue(ObjCIdType(ObjCPointer.Nullability.Nullable, emptyList()), kniFunction)
                ) { kotlinValues ->
                    val kotlinFunctionType = kotlinType.render(this.scope)
                    val kotlinFunction = "unwrapKotlinObjectHolder<$kotlinFunctionType>(${kotlinValues.last()})"
                    "$kotlinFunction(${kotlinValues.dropLast(1).joinToString()})"
                }.let {
                    codeBuilder.out("return $it;")
                }

                codeBuilder.lines.joinTo(this, separator = " ")

                append(" };") // Block ends.
                append(" })") // Statement expression ends.
            }
        }

        // When passing block pointer as Kotlin function from native to Kotlin,
        // it is converted to Kotlin function in [cFromBridged].

        override fun cToBridged(expr: NativeExpression): NativeExpression = expr

        override fun argFromBridged(
                expr: KotlinExpression,
                scope: KotlinScope,
                nativeBacked: NativeBacked
        ): KotlinExpression {
            val mappingBridgeGenerator = scope.mappingBridgeGenerator

            val funParameters = type.parameterTypes.mapIndexed { index, _ ->
                "p$index" to kotlinType.parameterTypes[index]
            }.joinToString { "${it.first}: ${it.second.render(scope)}" }

            val funReturnType = kotlinType.returnType.render(scope)

            val codeBuilder = KotlinCodeBuilder(scope)
            val kniBlockPtr = "kniBlockPtr"


            // Build the anonymous function expression:
            val anonymousFun = buildString {
                append("fun($funParameters): $funReturnType {\n") // Anonymous function begins.

                // As function body, generate the code which simply bridges to native and calls the block:
                mappingBridgeGenerator.kotlinToNative(
                        codeBuilder,
                        nativeBacked,
                        type.returnType,
                        type.parameterTypes.mapIndexed { index, it ->
                            TypedKotlinValue(it, "p$index")
                        } + TypedKotlinValue(PointerType(VoidType), "interpretCPointer<COpaque>($kniBlockPtr)")

                ) { nativeValues ->
                    val type = type
                    val blockType = blockTypeStringRepresentation(type)
                    val objCBlock = "((__bridge $blockType)${nativeValues.last()})"
                    "$objCBlock(${nativeValues.dropLast(1).joinToString()})"
                }.let {
                    codeBuilder.out("return $it")
                }

                codeBuilder.build().joinTo(this, separator = "\n")
                append("}") // Anonymous function ends.
            }

            val nullOutput = if (type.isNullable) "null" else "throw NullPointerException()"

            return "$expr.let { $kniBlockPtr -> if (kniBlockPtr == nativeNullPtr) $nullOutput else $anonymousFun }"
        }

        override fun constructPointedType(valueType: KotlinType): KotlinClassifierType {
            return Classifier.topLevel("kotlinx.cinterop", "ObjCBlockVar").typeWith(valueType)
        }
    }

    class ByRef(val pointed: KotlinType) : TypeInfo() {
        override fun argToBridged(expr: String) = error(pointed)
        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                error(pointed)
        override val bridgedType: BridgedType get() = error(pointed)
        override fun cFromBridged(expr: NativeExpression, scope: NativeScope, nativeBacked: NativeBacked) =
                error(pointed)
        override fun cToBridged(expr: String) = error(pointed)

        // TODO: this method must not exist
        override fun constructPointedType(valueType: KotlinType): KotlinClassifierType = error(pointed)
    }
}

fun mirrorPrimitiveType(type: PrimitiveType): TypeMirror.ByValue {
    val varClassName = when (type) {
        is CharType -> "ByteVar"
        is BoolType -> "BooleanVar"
        is IntegerType -> when (type.size) {
            1 -> "ByteVar"
            2 -> "ShortVar"
            4 -> "IntVar"
            8 -> "LongVar"
            else -> TODO(type.toString())
        }
        is FloatingType -> when (type.size) {
            4 -> "FloatVar"
            8 -> "DoubleVar"
            else -> TODO(type.toString())
        }
        else -> TODO(type.toString())
    }

    val varClass = Classifier.topLevel("kotlinx.cinterop", varClassName)
    val varClassOf = Classifier.topLevel("kotlinx.cinterop", "${varClassName}Of")

    val info = if (type == BoolType) {
        TypeInfo.Boolean()
    } else {
        TypeInfo.Primitive(type.bridgedType, varClassOf)
    }
    return TypeMirror.ByValue(varClass.type, info, type.kotlinType)
}

private fun byRefTypeMirror(pointedType: KotlinClassifierType) : TypeMirror.ByRef {
    val info = TypeInfo.ByRef(pointedType)
    return TypeMirror.ByRef(pointedType, info)
}

fun mirror(declarationMapper: DeclarationMapper, type: Type): TypeMirror = when (type) {
    is PrimitiveType -> mirrorPrimitiveType(type)

    is RecordType -> byRefTypeMirror(declarationMapper.getKotlinClassForPointed(type.decl).type)

    is EnumType -> {
        val pkg = declarationMapper.getPackageFor(type.def)
        val kotlinName = declarationMapper.getKotlinNameForValue(type.def)

        when {
            declarationMapper.isMappedToStrict(type.def) -> {
                val bridgedType = (type.def.baseType.unwrapTypedefs() as PrimitiveType).bridgedType
                val clazz = Classifier.topLevel(pkg, kotlinName)
                val info = TypeInfo.Enum(clazz, bridgedType)
                TypeMirror.ByValue(clazz.nested("Var").type, info, clazz.type)
            }
            !type.def.isAnonymous -> {
                val baseTypeMirror = mirror(declarationMapper, type.def.baseType)
                TypeMirror.ByValue(
                        Classifier.topLevel(pkg, kotlinName + "Var").type,
                        baseTypeMirror.info,
                        Classifier.topLevel(pkg, kotlinName).type
                )
            }
            else -> mirror(declarationMapper, type.def.baseType)
        }
    }

    is PointerType -> {
        val pointeeType = type.pointeeType
        val unwrappedPointeeType = pointeeType.unwrapTypedefs()
        if (unwrappedPointeeType is VoidType) {
            val info = TypeInfo.Pointer(KotlinTypes.cOpaque)
            TypeMirror.ByValue(KotlinTypes.cOpaquePointerVar, info, KotlinTypes.cOpaquePointer)
        } else if (unwrappedPointeeType is ArrayType) {
            mirror(declarationMapper, pointeeType)
        } else {
            val pointeeMirror = mirror(declarationMapper, pointeeType)
            val info = TypeInfo.Pointer(pointeeMirror.pointedType)
            TypeMirror.ByValue(
                    KotlinTypes.cPointerVar.typeWith(pointeeMirror.pointedType),
                    info,
                    KotlinTypes.cPointer.typeWith(pointeeMirror.pointedType)
            )
        }
    }

    is ArrayType -> {
        // TODO: array type doesn't exactly correspond neither to pointer nor to value.
        val elemTypeMirror = mirror(declarationMapper, type.elemType)
        if (type.elemType.unwrapTypedefs() is ArrayType) {
            elemTypeMirror
        } else {
            val info = TypeInfo.Pointer(elemTypeMirror.pointedType)
            TypeMirror.ByValue(
                    KotlinTypes.cArrayPointerVar.typeWith(elemTypeMirror.pointedType),
                    info,
                    KotlinTypes.cArrayPointer.typeWith(elemTypeMirror.pointedType)
            )
        }
    }

    is FunctionType -> byRefTypeMirror(KotlinTypes.cFunction.typeWith(getKotlinFunctionType(declarationMapper, type)))

    is Typedef -> {
        val baseType = mirror(declarationMapper, type.def.aliased)
        val pkg = declarationMapper.getPackageFor(type.def)

        val name = type.def.name
        when (baseType) {
            is TypeMirror.ByValue -> TypeMirror.ByValue(
                    Classifier.topLevel(pkg, "${name}Var").type,
                    baseType.info,
                    Classifier.topLevel(pkg, name).type,
                    nullable = baseType.nullable
            )

            is TypeMirror.ByRef -> TypeMirror.ByRef(Classifier.topLevel(pkg, name).type, baseType.info)
        }

    }

    is ObjCPointer -> objCPointerMirror(declarationMapper, type)

    else -> TODO(type.toString())
}

internal tailrec fun ObjCClass.isNSStringOrSubclass(): Boolean = when (this.name) {
    "NSMutableString", // fast path and handling for forward declarations.
    "NSString" -> true
    else -> {
        val baseClass = this.baseClass
        if (baseClass != null) {
            baseClass.isNSStringOrSubclass()
        } else {
            false
        }
    }
}

internal fun ObjCClass.isNSStringSubclass(): Boolean = this.baseClass?.isNSStringOrSubclass() == true

private fun objCPointerMirror(declarationMapper: DeclarationMapper, type: ObjCPointer): TypeMirror.ByValue {
    if (type is ObjCObjectPointer && type.def.isNSStringOrSubclass()) {
        val info = TypeInfo.NSString(type)
        return objCMirror(KotlinTypes.string, info, type.isNullable)
    }

    val clazz = when (type) {
        is ObjCIdType -> type.protocols.firstOrNull()?.let { declarationMapper.getKotlinClassFor(it) }
                ?: KotlinTypes.objCObject
        is ObjCClassPointer -> KotlinTypes.objCClass
        is ObjCObjectPointer -> declarationMapper.getKotlinClassFor(type.def)
        is ObjCInstanceType -> TODO(type.toString()) // Must have already been handled.
        is ObjCBlockPointer -> return objCBlockPointerMirror(declarationMapper, type)
    }

    val valueType = clazz.type
    return objCMirror(valueType, TypeInfo.ObjCPointerInfo(valueType, type), type.isNullable)
}

private fun objCBlockPointerMirror(declarationMapper: DeclarationMapper, type: ObjCBlockPointer): TypeMirror.ByValue {
    val returnType = if (type.returnType.unwrapTypedefs() is VoidType) {
        KotlinTypes.unit
    } else {
        mirror(declarationMapper, type.returnType).argType
    }
    val kotlinType = KotlinFunctionType(
            type.parameterTypes.map { mirror(declarationMapper, it).argType },
            returnType
    )

    val info = TypeInfo.ObjCBlockPointerInfo(kotlinType, type)
    return objCMirror(kotlinType, info, type.isNullable)
}

private fun objCMirror(valueType: KotlinType, info: TypeInfo, nullable: Boolean) = TypeMirror.ByValue(
        info.constructPointedType(valueType.makeNullableAsSpecified(nullable)),
        info,
        valueType.makeNullable(), // All typedefs to Objective-C pointers would be nullable for simplicity
        nullable
)

fun getKotlinFunctionType(declarationMapper: DeclarationMapper, type: FunctionType): KotlinFunctionType {
    val returnType = if (type.returnType.unwrapTypedefs() is VoidType) {
        KotlinTypes.unit
    } else {
        mirror(declarationMapper, type.returnType).argType
    }
    return KotlinFunctionType(
            type.parameterTypes.map { mirror(declarationMapper, it).argType },
            returnType,
            nullable = false
    )
}

