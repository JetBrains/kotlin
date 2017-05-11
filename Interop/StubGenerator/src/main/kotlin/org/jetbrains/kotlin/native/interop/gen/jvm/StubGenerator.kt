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

package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.indexer.*
import java.lang.IllegalStateException

enum class KotlinPlatform {
    JVM,
    NATIVE
}

private class StubsFragment(val kotlinStubLines: List<String>, val nativeStubLines: List<String> = emptyList())

// TODO: mostly rename 'jni' to 'c'.

class StubGenerator(
        val nativeIndex: NativeIndex,
        val configuration: InteropConfiguration,
        val libName: String,
        val dumpShims: Boolean,
        val verbose: Boolean = false,
        val platform: KotlinPlatform = KotlinPlatform.JVM) {

    private fun log(message: String) {
        if (verbose) {
            println(message)
        }
    }

    val pkgName: String
        get() = configuration.pkgName

    private val jvmFileClassName = if (pkgName.isEmpty()) {
        libName
    } else {
        pkgName.substringAfterLast('.')
    }

    val excludedFunctions: Set<String>
        get() = configuration.excludedFunctions

    val keywords = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
            "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "val", "var", "when", "while"
    )

    /**
     * For this identifier constructs the string to be parsed by Kotlin as `SimpleName`
     * defined [here](https://kotlinlang.org/docs/reference/grammar.html#SimpleName).
     */
    fun String.asSimpleName(): String {
        return if (this in keywords) {
            "`$this`"
        } else {
            this
        }
    }

    /**
     * The names that should not be used for struct classes to prevent name clashes
     */
    val forbiddenStructNames = run {
        val functionNames = nativeIndex.functions.map { it.name }
        val fieldNames = nativeIndex.structs.mapNotNull { it.def }.flatMap { it.fields }.map { it.name }
        (functionNames + fieldNames).toSet() + keywords
    }

    val StructDecl.isAnonymous: Boolean
        get() = spelling.contains("(anonymous ") // TODO: it is a hack

    val anonymousStructKotlinNames = mutableMapOf<StructDecl, String>()

    /**
     * The name to be used for this struct in Kotlin
     */
    val StructDecl.kotlinName: String
        get() {
            if (this.isAnonymous) {
                val names = anonymousStructKotlinNames
                return names.getOrPut(this) {
                    "anonymousStruct${names.size + 1}"
                }
            }

            val strippedCName = if (spelling.startsWith("struct ") || spelling.startsWith("union ")) {
                spelling.substringAfter(' ')
            } else {
                spelling
            }

            return if (strippedCName !in forbiddenStructNames) strippedCName else (strippedCName + "Struct")
        }

    val EnumDef.isAnonymous: Boolean
        get() = spelling.contains("(anonymous ") // TODO: it is a hack

    /**
     * Indicates whether this enum should be represented as Kotlin enum.
     */
    val EnumDef.isStrictEnum: Boolean
            // TODO: if an anonymous enum defines e.g. a function return value or struct field type,
            // then it probably should be represented as Kotlin enum
        get() {
            if (this.isAnonymous) {
                return false
            }

            val name = this.kotlinName

            if (name in configuration.strictEnums) {
                return true
            }

            if (name in configuration.nonStrictEnums) {
                return false
            }

            // Let the simple heuristic decide:
            return !this.constants.any { it.isExplicitlyDefined }
        }

    /**
     * The name to be used for this enum in Kotlin
     */
    val EnumDef.kotlinName: String
        get() = if (spelling.startsWith("enum ")) {
            spelling.substringAfter(' ')
        } else {
            assert (!isAnonymous)
            spelling
        }

    val RecordType.kotlinName: String
        get() = decl.kotlinName


    val functionsToBind = nativeIndex.functions.filter { it.name !in excludedFunctions }

    private val macroConstantsByName = nativeIndex.macroConstants.associateBy { it.name }

    /**
     * The output currently used by the generator.
     * Should append line separator after any usage.
     */
    private var out: (String) -> Unit = {
        throw IllegalStateException()
    }

    fun <R> withOutput(output: (String) -> Unit, action: () -> R): R {
        val oldOut = out
        out = output
        try {
            return action()
        } finally {
            out = oldOut
        }
    }

    fun generateLinesBy(action: () -> Unit): List<String> {
        val result = mutableListOf<String>()
        withOutput({ result.add(it) }, action)
        return result
    }

    fun <R> withOutput(appendable: Appendable, action: () -> R): R {
        return withOutput({ appendable.appendln(it) }, action)
    }

    private fun generateKotlinFragmentBy(block: () -> Unit): StubsFragment {
        val lines = generateLinesBy(block)
        return StubsFragment(kotlinStubLines = lines)
    }

    private fun <R> indent(action: () -> R): R {
        val oldOut = out
        return withOutput({ oldOut("    $it") }, action)
    }

    private fun <R> block(header: String, body: () -> R): R {
        out("$header {")
        val res = indent {
            body()
        }
        out("}")
        return res
    }

    /**
     * Returns the expression which could be used for this type in C code.
     *
     * TODO: use libclang to implement:
     */
    fun Type.getStringRepresentation(): String {
        return when (this) {
            is VoidType -> "void"
            is CharType -> "char"
            is IntegerType -> this.spelling
            is FloatingType -> this.spelling

            is PointerType -> {
                val pointeeType = this.pointeeType
                if (pointeeType is FunctionType) {
                    "void*" // TODO
                } else {
                    pointeeType.getStringRepresentation() + "*"
                }
            }

            is RecordType -> this.decl.spelling

            is ArrayType -> "void*" // TODO

            is EnumType -> if (this.def.isAnonymous) {
                this.def.baseType.getStringRepresentation()
            } else {
                this.def.spelling
            }

            is Typedef -> this.def.name

            else -> throw kotlin.NotImplementedError()
        }
    }

    val PrimitiveType.kotlinType: String
        get() = when (this) {
            is CharType -> "Byte"

            // TODO: C primitive types should probably be generated as type aliases for Kotlin types.
            is IntegerType -> when (this.size) {
                1 -> "Byte"
                2 -> "Short"
                4 -> "Int"
                8 -> "Long"
                else -> TODO(this.toString())
            }

            is FloatingType -> when (this.size) {
                4 -> "Float"
                8 -> "Double"
                else -> TODO(this.toString())
            }

            else -> throw NotImplementedError()
        }

    /**
     * Describes the Kotlin types used to represent some C type.
     */
    private sealed class TypeMirror(val pointedTypeName: String, val info: TypeInfo) {
        /**
         * Type to be used in bindings for argument or return value.
         */
        abstract val argType: String

        /**
         * Mirror for C type to be represented in Kotlin as by-value type.
         */
        class ByValue(pointedTypeName: String, info: TypeInfo, val valueTypeName: String) :
                TypeMirror(pointedTypeName, info) {

            override val argType: String
                get() = valueTypeName + if (info is TypeInfo.Pointer) "?" else ""
        }

        /**
         * Mirror for C type to be represented in Kotlin as by-ref type.
         */
        class ByRef(pointedTypeName: String, info: TypeInfo) : TypeMirror(pointedTypeName, info) {
            override val argType: String
                get() = pointedTypeName
        }
    }

    /**
     * Describes various type conversions for [TypeMirror].
     */
    private sealed class TypeInfo {
        /**
         * The conversion from [TypeMirror.argType] to [jniType].
         */
        abstract fun argToJni(name: String): String

        /**
         * The conversion from [jniType] to [TypeMirror.argType].
         */
        abstract fun argFromJni(name: String): String

        /**
         * The type to be used for passing [TypeMirror.argType] through JNI.
         */
        abstract val jniType: String

        /**
         * If this info is for [TypeMirror.ByValue], then this method describes how to
         * construct pointed-type from value type.
         */
        abstract fun constructPointedType(valueType: String): String

        class Primitive(override val jniType: String, val varTypeName: String) : TypeInfo() {

            override fun argToJni(name: String) = name
            override fun argFromJni(name: String) = name

            override fun constructPointedType(valueType: String) = "${varTypeName}Of<$valueType>"
        }

        class Enum(val className: String, val baseType: String) : TypeInfo() {
            override fun argToJni(name: String) = "$name.value"

            override fun argFromJni(name: String) = "$className.byValue($name)"

            override val jniType: String
                get() = baseType

            override fun constructPointedType(valueType: String) = "$className.Var" // TODO: improve

        }

        class Pointer(val pointee: String) : TypeInfo() {
            override fun argToJni(name: String) = "$name.rawValue"

            override fun argFromJni(name: String) = "interpretCPointer<$pointee>($name)"

            override val jniType: String
                get() = "NativePtr"

            override fun constructPointedType(valueType: String) = "CPointerVarOf<$valueType>"
        }

        class ByRef(val pointed: String) : TypeInfo() {
            override fun argToJni(name: String) = "$name.rawPtr"

            override fun argFromJni(name: String) = "interpretPointed<$pointed>($name)"

            override val jniType: String
                get() = "NativePtr"

            override fun constructPointedType(valueType: String): String {
                // TODO: this method must not exist
                throw UnsupportedOperationException()
            }
        }
    }

    private fun mirror(type: PrimitiveType): TypeMirror.ByValue {
        val varTypeName = when (type) {
            is CharType -> "ByteVar"
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

        val info = TypeInfo.Primitive(type.kotlinType, varTypeName)
        return TypeMirror.ByValue(varTypeName, info, type.kotlinType)
    }

    private fun byRefTypeMirror(pointedTypeName: String) : TypeMirror.ByRef {
        val info = TypeInfo.ByRef(pointedTypeName)
        return TypeMirror.ByRef(pointedTypeName, info)
    }

    private fun mirror(type: Type): TypeMirror = when (type) {
        is PrimitiveType -> mirror(type)

        is RecordType -> byRefTypeMirror(type.kotlinName.asSimpleName())

        is EnumType -> when {
            type.def.isStrictEnum -> {
                val classSimpleName = type.def.kotlinName.asSimpleName()
                val info = TypeInfo.Enum(classSimpleName, type.def.baseType.kotlinType)
                TypeMirror.ByValue("$classSimpleName.Var", info, classSimpleName)
            }
            !type.def.isAnonymous -> {
                val baseTypeMirror = mirror(type.def.baseType)
                val name = type.def.kotlinName
                TypeMirror.ByValue("${name}Var", baseTypeMirror.info, name.asSimpleName())
            }
            else -> mirror(type.def.baseType)
        }

        is PointerType -> {
            val pointeeType = type.pointeeType
            val unwrappedPointeeType = pointeeType.unwrapTypedefs()
            if (unwrappedPointeeType is VoidType) {
                val info = TypeInfo.Pointer("COpaque")
                TypeMirror.ByValue("COpaquePointerVar", info, "COpaquePointer")
            } else if (unwrappedPointeeType is ArrayType) {
                mirror(pointeeType)
            } else {
                val pointeeMirror = mirror(pointeeType)
                val info = TypeInfo.Pointer(pointeeMirror.pointedTypeName)
                TypeMirror.ByValue("CPointerVar<${pointeeMirror.pointedTypeName}>", info,
                        "CPointer<${pointeeMirror.pointedTypeName}>")
            }
        }

        is ArrayType -> {
            // TODO: array type doesn't exactly correspond neither to pointer nor to value.
            val elemTypeMirror = mirror(type.elemType)
            if (type.elemType.unwrapTypedefs() is ArrayType) {
                elemTypeMirror
            } else {
                val info = TypeInfo.Pointer(elemTypeMirror.pointedTypeName)
                TypeMirror.ByValue("CArrayPointerVar<${elemTypeMirror.pointedTypeName}>", info,
                        "CArrayPointer<${elemTypeMirror.pointedTypeName}>")
            }
        }

        is FunctionType -> byRefTypeMirror("CFunction<${getKotlinFunctionType(type)}>")

        is Typedef -> {
            val baseType = mirror(type.def.aliased)
            val name = type.def.name
            when (baseType) {
                is TypeMirror.ByValue -> TypeMirror.ByValue("${name}Var", baseType.info, name.asSimpleName())
                is TypeMirror.ByRef -> TypeMirror.ByRef(name.asSimpleName(), baseType.info)
            }

        }

        else -> TODO(type.toString())
    }

    /**
     * Describes how to represent in Kotlin the value to be passed to native code.
     *
     * @param kotlinType the name of Kotlin type to be used for this value in Kotlin.
     * @param kotlinConv the function such that `kotlinConv(name)` converts variable `name` to pass it into JNI stub
     * @param memScoped the conversion allocates memory and should be placed into `memScoped {}` block
     * @param kotlinJniBridgeType the name of Kotlin type to be used for this value in JNI stub
     */
    class OutValueBinding(val kotlinType: String,
                          val kotlinConv: ((String) -> String)? = null,
                          val memScoped: Boolean = false,
                          val kotlinJniBridgeType: String = kotlinType)

    /**
     * Describes how to represent in Kotlin the value to be received from native code.
     *
     * @param kotlinJniBridgeType the name of Kotlin type to be used for this value in JNI stub
     * @param conv the function such that `conv(name)` converts variable `name` received from JNI stub to `kotlinType`.
     * @param kotlinType the name of Kotlin type to be used for this value in Kotlin.
     */
    class InValueBinding(val kotlinJniBridgeType: String,
                         val conv: ((String) -> String) = { it },
                         val kotlinType: String = kotlinJniBridgeType)

    /**
     * Constructs [OutValueBinding] for the value of given C type.
     */
    fun getOutValueBinding(type: Type): OutValueBinding {
        if (type.unwrapTypedefs() is RecordType) {
            // TODO: this case should probably be handled more generally.
            val typeMirror = mirror(type)
            return OutValueBinding(
                    kotlinType = "CValue<${typeMirror.pointedTypeName}>",
                    kotlinConv = { name -> "$name.getPointer(memScope).rawValue" }, // TODO: eliminate this copying
                    memScoped = true,
                    kotlinJniBridgeType = "NativePtr"
            )
        }

        val mirror = mirror(type)

        return OutValueBinding(
                kotlinType = mirror.argType,
                kotlinConv = mirror.info::argToJni,
                kotlinJniBridgeType = mirror.info.jniType
        )
    }

    fun representCFunctionParameterAsValuesRef(type: Type): Type? {
        val pointeeType = when (type) {
            is PointerType -> type.pointeeType
            is ArrayType -> type.elemType
            else -> return null
        }

        val unwrappedPointeeType = pointeeType.unwrapTypedefs()
        if (unwrappedPointeeType is VoidType || unwrappedPointeeType is FunctionType) {
            // Passing `void`s or function by value is not very sane:
            return null
        }

        if (unwrappedPointeeType is ArrayType) {
            return representCFunctionParameterAsValuesRef(pointeeType)
        }

        return pointeeType
    }

    fun representCFunctionParameterAsString(type: Type): Boolean {
        val unwrappedType = type.unwrapTypedefs()
        return unwrappedType is PointerType && unwrappedType.pointeeIsConst &&
                unwrappedType.pointeeType.unwrapTypedefs() == CharType
    }

    fun getCFunctionParamBinding(type: Type): OutValueBinding {
        if (representCFunctionParameterAsString(type)) {
            return OutValueBinding(
                    kotlinType = "String?", // TODO: mention the C type (e.g. with annotation).
                    kotlinConv = { name -> "$name?.cstr?.getPointer(memScope).rawValue" },
                    memScoped = true,
                    kotlinJniBridgeType = "NativePtr"
            )
        }

        representCFunctionParameterAsValuesRef(type)?.let {
            val pointeeMirror = mirror(it)
            return OutValueBinding(
                    kotlinType = "CValuesRef<${pointeeMirror.pointedTypeName}>?",
                    kotlinConv = { name -> "$name?.getPointer(memScope).rawValue" },
                    memScoped = true,
                    kotlinJniBridgeType = "NativePtr"
            )
        }

        return getOutValueBinding(type)
    }

    fun getCallbackRetValBinding(type: Type): OutValueBinding {
        if (type.unwrapTypedefs() is VoidType) {
            return OutValueBinding(
                    kotlinType = "Unit",
                    kotlinConv = { throw UnsupportedOperationException() },
                    kotlinJniBridgeType = "void"
            )
        }

        return getOutValueBinding(type)
    }

    /**
     * Constructs [InValueBinding] for the value of given C type.
     */
    fun getInValueBinding(type: Type): InValueBinding  {
        if (type.unwrapTypedefs() is RecordType) {
            val typeMirror = mirror(type)
            return InValueBinding(
                    kotlinJniBridgeType = "NativePtr",
                    conv = { name -> "interpretPointed<${typeMirror.pointedTypeName}>($name).readValue()" },
                    kotlinType = "CValue<${typeMirror.pointedTypeName}>"
            )
        }

        val mirror = mirror(type)

        return InValueBinding(
                kotlinJniBridgeType = mirror.info.jniType,
                conv = mirror.info::argFromJni,
                kotlinType = mirror.argType
        )
    }

    fun getCFunctionRetValBinding(func: FunctionDecl): InValueBinding {
        when {
            func.returnsVoid() -> return InValueBinding("Unit")
        }

        return getInValueBinding(func.returnType)
    }

    fun getCallbackParamBinding(type: Type): InValueBinding {
        return getInValueBinding(type)
    }

    private fun getKotlinFunctionType(type: FunctionType): String {
        return "(" +
                type.parameterTypes.map { getCallbackParamBinding(it).kotlinType }.joinToString(", ") +
                ") -> " +
                getCallbackRetValBinding(type.returnType).kotlinType
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

    /**
     * Produces to [out] the definition of Kotlin class representing the reference to given struct.
     */
    private fun generateStruct(decl: StructDecl) {
        val def = decl.def
        if (def == null) {
            generateForwardStruct(decl)
            return
        }

        if (platform == KotlinPlatform.JVM) {
            if (def.hasNaturalLayout) {
                out("@CNaturalStruct(${def.fields.joinToString { it.name.quoteAsKotlinLiteral() }})")
            }
        }

        block("class ${decl.kotlinName.asSimpleName()}(override val rawPtr: NativePtr) : CStructVar()") {
            out("")
            out("companion object : Type(${def.size}, ${def.align})") // FIXME: align
            out("")
            for (field in def.fields) {
                if (field.name.isEmpty()) continue

                try {
                    if (field.offset < 0) throw NotImplementedError();
                    assert(field.offset % 8 == 0L)
                    val offset = field.offset / 8
                    val fieldRefType = mirror(field.type)
                    val unwrappedFieldType = field.type.unwrapTypedefs()
                    if (unwrappedFieldType is ArrayType) {
                        val type = (fieldRefType as TypeMirror.ByValue).valueTypeName

                        if (platform == KotlinPlatform.JVM) {
                            val length = getArrayLength(unwrappedFieldType)

                            // TODO: @CLength should probably be used on types instead of properties.
                            out("@CLength($length)")
                        }

                        out("val ${field.name.asSimpleName()}: $type")
                        out("    get() = arrayMemberAt($offset)")
                    } else {
                        if (fieldRefType is TypeMirror.ByValue) {
                            val pointedTypeName = fieldRefType.pointedTypeName
                            out("var ${field.name.asSimpleName()}: ${fieldRefType.argType}")
                            out("    get() = memberAt<$pointedTypeName>($offset).value")
                            out("    set(value) { memberAt<$pointedTypeName>($offset).value = value }")
                        } else {
                            out("val ${field.name.asSimpleName()}: ${fieldRefType.pointedTypeName}")
                            out("    get() = memberAt($offset)")
                        }
                    }
                    out("")
                } catch (e: Throwable) {
                    log("Warning: cannot generate definition for field ${decl.kotlinName}.${field.name}")
                }
            }
        }
    }

    /**
     * Produces to [out] the definition of Kotlin class representing the reference to given forward (incomplete) struct.
     */
    private fun generateForwardStruct(s: StructDecl) {
        out("class ${s.kotlinName.asSimpleName()}(override val rawPtr: NativePtr) : COpaque")
    }

    /**
     * Produces to [out] the Kotlin definitions for given enum.
     */
    private fun generateEnum(e: EnumDef) {
        if (!e.isStrictEnum) {
            generateEnumAsConstants(e)
            return
        }

        val baseTypeMirror = mirror(e.baseType)

        block("enum class ${e.kotlinName.asSimpleName()}(override val value: ${e.baseType.kotlinType}) : CEnum") {
            e.constants.forEach {
                out("${it.name.asSimpleName()}(${it.value}),")
            }
            out(";")
            out("")
            block("companion object") {
                out("fun byValue(value: ${e.baseType.kotlinType}) = " +
                        "${e.kotlinName.asSimpleName()}.values().find { it.value == value }!!")
            }
            out("")
            block("class Var(override val rawPtr: NativePtr) : CEnumVar()") {
                out("companion object : Type(${baseTypeMirror.pointedTypeName}.size.toInt())")
                out("var value: ${e.kotlinName.asSimpleName()}")
                out("    get() = byValue(this.reinterpret<${baseTypeMirror.pointedTypeName}>().value)")
                out("    set(value) { this.reinterpret<${baseTypeMirror.pointedTypeName}>().value = value.value }")
            }
        }
    }

    /**
     * Produces to [out] the Kotlin definitions for given enum which shouldn't be represented as Kotlin enum.
     *
     * @see isStrictEnum
     */
    private fun generateEnumAsConstants(e: EnumDef) {
        // TODO: if this enum defines e.g. a type of struct field, then it should be generated inside the struct class
        // to prevent name clashing

        val constants = e.constants.filter {
            // Macro "overrides" the original enum constant.
            it.name !in macroConstantsByName
        }

        val typeName: String

        if (e.isAnonymous) {
            if (constants.isNotEmpty()) {
                out("// ${e.spelling}:")
            }

            typeName = e.baseType.kotlinType
        } else {
            val typeMirror = mirror(EnumType(e))
            if (typeMirror !is TypeMirror.ByValue) {
                error("unexpected enum type mirror: $typeMirror")
            }

            // Generate as typedef:
            val varTypeName = typeMirror.info.constructPointedType(typeMirror.valueTypeName)
            out("typealias ${typeMirror.pointedTypeName} = $varTypeName")
            out("typealias ${typeMirror.valueTypeName} = ${e.baseType.kotlinType}")

            if (constants.isNotEmpty()) {
                out("")
            }

            typeName = typeMirror.valueTypeName
        }

        for (constant in constants) {
            out("val ${constant.name.asSimpleName()}: $typeName = ${constant.value}")
        }
    }

    private fun generateTypedef(def: TypedefDef) {
        val mirror = mirror(Typedef(def))
        val baseMirror = mirror(def.aliased)

        when (baseMirror) {
            is TypeMirror.ByValue -> {
                val valueTypeName = (mirror as TypeMirror.ByValue).valueTypeName
                val varTypeAliasee = mirror.info.constructPointedType(valueTypeName)
                out("typealias ${mirror.pointedTypeName} = $varTypeAliasee")
                out("typealias $valueTypeName = ${baseMirror.valueTypeName}")
            }
            is TypeMirror.ByRef -> {
                out("typealias ${mirror.pointedTypeName} = ${baseMirror.pointedTypeName}")
            }
        }
    }

    private fun generateStubsForFunction(func: FunctionDecl): StubsFragment {
        val kotlinStubLines = generateLinesBy {
            generateKotlinBindingMethod(func)
            if (func.requiresKotlinAdapter()) {
                out("")
                generateKotlinExternalMethod(func)
            }
        }

        val nativeStubLines = generateLinesBy {
            generateCJniFunction(func)
        }

        return StubsFragment(kotlinStubLines, nativeStubLines)
    }

    private fun generateStubsForFunctions(functions: List<FunctionDecl>): List<StubsFragment> {
        val stubs = functions.mapNotNull {
            try {
                generateStubsForFunction(it)
            } catch (e: Throwable) {
                log("Warning: cannot generate stubs for function ${it.name}")
                null
            }
        }

        return stubs.map { it.nativeStubLines }
                .mapFragmentIsCompilable(libraryForCStubs)
                .mapIndexedNotNull { index, stubIsCompilable ->
                    if (stubIsCompilable) {
                        stubs[index]
                    } else {
                        null
                    }
                }
    }

    private fun FunctionDecl.generateAsFfiVarargs(): Boolean = (platform == KotlinPlatform.NATIVE && this.isVararg &&
            // Neither takes nor returns structs by value:
            !this.returnsRecord() && this.parameters.all { it.type.unwrapTypedefs() !is RecordType })

    /**
     * Constructs [InValueBinding] for return value of Kotlin binding for given C function.
     */
    private fun retValBinding(func: FunctionDecl) = getCFunctionRetValBinding(func)

    private fun FunctionDecl.returnsRecord(): Boolean = this.returnType.unwrapTypedefs() is RecordType
    private fun FunctionDecl.returnsVoid(): Boolean = this.returnType.unwrapTypedefs() is VoidType

    /**
     * Constructs [OutValueBinding]s for parameters of Kotlin binding for given C function.
     */
    private fun paramBindings(func: FunctionDecl): Array<OutValueBinding> {
        val paramBindings = func.parameters.map { param ->
            getCFunctionParamBinding(param.type)
        }.toMutableList()

        if (func.returnsRecord()) {
            paramBindings.add(OutValueBinding(
                    kotlinType = "should not be used",
                    kotlinConv = { throw UnsupportedOperationException() },
                    kotlinJniBridgeType = "NativePtr"
            ))
        }

        return paramBindings.toTypedArray()
    }

    /**
     * Returns names for parameters of Kotlin binding for given C function.
     */
    private fun paramNames(func: FunctionDecl): Array<String> {
        val paramNames = func.parameters.mapIndexed { i: Int, parameter: Parameter ->
            val name = parameter.name
            if (name != null && name != "") {
                name
            } else {
                "arg$i"
            }
        }.toMutableList()

        if (func.returnsRecord()) {
            paramNames.add("retValPlacement")
        }

        return paramNames.toTypedArray()
    }

    /**
     * Produces to [out] the definition of Kotlin binding for given C function.
     */
    private fun generateKotlinBindingMethod(func: FunctionDecl) {
        val paramNames = paramNames(func).toList().let {
            if (func.returnsRecord()) {
                // The last parameter should be present only in C adapter.
                it.dropLast(1)
            } else {
                it
            }
        }
        val paramBindings = paramBindings(func)
        val retValBinding = retValBinding(func)

        val args = paramNames.mapIndexed { i: Int, name: String ->
            "${name.asSimpleName()}: " + paramBindings[i].kotlinType
        }.joinToString(", ")

        if (func.generateAsFfiVarargs()) {
            val returnTypeKind = getFfiTypeKind(func.returnType)

            val variadicParameter = "vararg variadicArguments: Any?"
            val allParameters = if (args.isEmpty()) variadicParameter else "$args, $variadicParameter"
            val header = "fun ${func.name.asSimpleName()}($allParameters): ${retValBinding.kotlinType} = memScoped"
            val returnValueMirror = mirror(func.returnType)
            block(header) {
                val resultPtr = if (!func.returnsVoid()) {
                    val returnType = returnValueMirror.pointedTypeName
                    out("val resultVar = allocFfiReturnValueBuffer<$returnType>(typeOf<$returnType>())")
                    "resultVar.rawPtr"
                } else {
                    "nativeNullPtr"
                }
                val fixedArguments = paramNames.joinToString(", ") { it.asSimpleName() }
                out("callWithVarargs(${func.kotlinExternalName}(), $resultPtr, $returnTypeKind, " +
                        "arrayOf($fixedArguments), variadicArguments, memScope)")

                if (!func.returnsVoid()) {
                    out("resultVar.value")
                }
            }
            return
        }

        val header = "fun ${func.name.asSimpleName()}($args): ${retValBinding.kotlinType}"

        if (!func.requiresKotlinAdapter()) {
            assert(platform == KotlinPlatform.NATIVE)
            out(func.symbolNameAnnotation)
            out("external $header")
            return
        }

        fun generateBody(memScoped: Boolean) {
            val arguments = paramNames.mapIndexed { i: Int, name: String ->
                val binding = paramBindings[i]
                val externalParamName: String

                if (binding.kotlinConv != null) {
                    externalParamName = "_$name"
                    out("val $externalParamName = " + binding.kotlinConv.invoke(name.asSimpleName()))
                } else {
                    externalParamName = name.asSimpleName()
                }

                externalParamName

            }.toMutableList()

            if (func.returnsRecord()) {
                val retValMirror = mirror(func.returnType)
                arguments.add("alloc<${retValMirror.pointedTypeName}>().rawPtr")
            }

            val callee = func.kotlinExternalName
            out("val res = $callee(" + arguments.joinToString(", ") + ")")

            val result = retValBinding.conv("res")
            if (dumpShims) {
                val returnValueRepresentation  = result
                out("print(\"\${${returnValueRepresentation}}\\t= \")")
                out("print(\"${func.name}( \")")
                val argsRepresentation = paramNames.map{"\${${it}}"}.joinToString(", ")
                out("print(\"${argsRepresentation}\")")
                out("println(\")\")")
            }

            if (memScoped) {
                out(result)
            } else {
                out("return " + result)
            }
        }

        block(header) {
            val memScoped = paramBindings.any { it.memScoped } || func.returnsRecord()
            if (memScoped) {
                block("return memScoped") {
                    generateBody(true)
                }
            } else {
                generateBody(false)
            }
        }
    }

    private fun getFfiTypeKind(type: Type): String {
        val unwrappedType = type.unwrapTypedefs()
        return when (unwrappedType) {
            is VoidType -> "FFI_TYPE_KIND_VOID"
            is PointerType -> "FFI_TYPE_KIND_POINTER"
            is IntegerType -> when (unwrappedType.size) {
                1 -> "FFI_TYPE_KIND_SINT8"
                2 -> "FFI_TYPE_KIND_SINT16"
                4 -> "FFI_TYPE_KIND_SINT32"
                8 -> "FFI_TYPE_KIND_SINT64"
                else -> TODO(unwrappedType.toString())
            }
            is FloatingType -> when (unwrappedType.size) {
                4 -> "FFI_TYPE_KIND_FLOAT"
                8 -> "FFI_TYPE_KIND_DOUBLE"
                else -> TODO(unwrappedType.toString())
            }
            is EnumType -> getFfiTypeKind(unwrappedType.def.baseType)
            else -> TODO(unwrappedType.toString())
        }
    }

    /**
     * Returns the expression to be parsed by Kotlin as string literal with given contents,
     * i.e. transforms `foo$bar` to `"foo\$bar"`.
     */
    private fun String.quoteAsKotlinLiteral(): String {
        val sb = StringBuilder()
        sb.append('"')

        this.forEach { c ->
            val escaped = when (c) {
                in 'a' .. 'z', in 'A' .. 'Z', in '0' .. '9', '_' -> c.toString()
                '$' -> "\\$"
                // TODO: improve result readability by preserving more characters.
                else -> "\\u" + "%04X".format(c.toInt())
            }
            sb.append(escaped)
        }

        sb.append('"')
        return sb.toString()
    }

    /**
     * Returns `true` iff the function binding for Kotlin Native
     * requires non-trivial Kotlin adapter to convert arguments.
     */
    private fun FunctionDecl.requiresKotlinAdapter(): Boolean {
        // TODO: restore this optimization after refactoring stub generator.
        return true
    }

    /**
     * Returns `true` iff the function binding for Kotlin Native
     * requires non-trivial C adapter to convert arguments.
     */
    private fun FunctionDecl.requiresCAdapter(): Boolean {
        if (platform != KotlinPlatform.NATIVE) {
            return true
        }

        return true
    }

    private fun integerLiteral(type: Type, value: Long): String? {
        if (value == Long.MIN_VALUE) {
            return "${value + 1} - 1" // Workaround for "The value is out of range" compile error.
        }

        val unwrappedType = type.unwrapTypedefs()
        if (unwrappedType !is PrimitiveType) {
            return null
        }

        val narrowedValue: Number = when (unwrappedType.kotlinType) {
            "Byte" -> value.toByte()
            "Short" -> value.toShort()
            "Int" -> value.toInt()
            "Long" -> value
            else -> return null
        }

        return narrowedValue.toString()
    }

    private fun floatingLiteral(type: Type, value: Double): String? {
        val unwrappedType = type.unwrapTypedefs()
        if (unwrappedType !is FloatingType) return null
        return when (unwrappedType.size) {
            4 -> {
                val floatValue = value.toFloat()
                val bits = java.lang.Float.floatToRawIntBits(floatValue)
                "bitsToFloat($bits) /* == $floatValue */"
            }
            8 -> {
                val bits = java.lang.Double.doubleToRawLongBits(value)
                "bitsToDouble($bits) /* == $value */"
            }
            else -> null
        }
    }

    private fun generateConstant(constant: ConstantDef) {
        val literal = when (constant) {
            is IntegerConstantDef -> integerLiteral(constant.type, constant.value) ?: return
            is FloatingConstantDef -> floatingLiteral(constant.type, constant.value) ?: return
            else -> {
                // Not supported yet, ignore:
                return
            }
        }

        val kotlinType = mirror(constant.type).argType

        // TODO: improve value rendering.

        // TODO: consider using `const` modifier.
        // It is not currently possible for floating literals.
        // Also it provokes constant propagation which can reduce binary compatibility
        // when replacing interop stubs without recompiling the application.

        out("val ${constant.name.asSimpleName()}: $kotlinType = $literal")
    }

    /**
     * The name of C adapter to be used for the function binding for Kotlin Native.
     */
    private val FunctionDecl.cStubName: String
        get() {
            require(platform == KotlinPlatform.NATIVE)
            return pkgName.replace('.', '_') + "_kni_" + this.name
        }

    private val FunctionDecl.kotlinExternalName: String
        get() {
            require(this.requiresKotlinAdapter())
            return "kni_$name"
        }

    /**
     * The annotation for an external function on Kotlin Native
     * to bind it to either the C function itself or the C adapter.
     */
    private val FunctionDecl.symbolNameAnnotation: String
        get() {
            require(platform == KotlinPlatform.NATIVE)
            val symbolName = if (requiresCAdapter()) {
                cStubName
            } else {
                // `@SymbolName` annotation on Kotlin Native defines the LLVM function name
                // which is assumed to match the C function name.
                // However on some platforms (e.g. macOS) C function names are being mangled.
                // Using `0x01` prefix prevents LLVM from mangling the name.
                "\u0001${binaryName}"
            }
            return "@SymbolName(${symbolName.quoteAsKotlinLiteral()})"
        }

    /**
     * Produces to [out] the definition of Kotlin JNI function used in binding for given C function.
     */
    private fun generateKotlinExternalMethod(func: FunctionDecl) {
        if (func.generateAsFfiVarargs()) {
            assert (platform == KotlinPlatform.NATIVE)
            // The C stub simply returns pointer to the function:
            out(func.symbolNameAnnotation)
            out("private external fun ${func.kotlinExternalName}(): NativePtr")
            return
        }

        val paramNames = paramNames(func)
        val paramBindings = paramBindings(func)
        val retValBinding = retValBinding(func)

        val args = paramNames.mapIndexed { i: Int, name: String ->
            "${name.asSimpleName()}: " + paramBindings[i].kotlinJniBridgeType
        }.joinToString(", ")

        if (platform == KotlinPlatform.NATIVE) {
            out(func.symbolNameAnnotation)
        }
        out("private external fun ${func.kotlinExternalName}($args): ${retValBinding.kotlinJniBridgeType}")
    }

    private fun generateStubs(): List<StubsFragment> {
        val stubs = mutableListOf<StubsFragment>()

        stubs.addAll(generateStubsForFunctions(functionsToBind))

        nativeIndex.macroConstants.forEach {
            try {
                stubs.add(
                        generateKotlinFragmentBy { generateConstant(it) }
                )
            } catch (e: Throwable) {
                log("Warning: cannot generate stubs for constant ${it.name}")
            }
        }

        nativeIndex.structs.forEach { s ->
            try {
                stubs.add(
                    generateKotlinFragmentBy { generateStruct(s) }
                )
            } catch (e: Throwable) {
                log("Warning: cannot generate definition for struct ${s.kotlinName}")
            }
        }

        nativeIndex.enums.forEach {
            try {
                stubs.add(
                        generateKotlinFragmentBy { generateEnum(it) }
                )
            } catch (e: Throwable) {
                log("Warning: cannot generate definition for enum ${it.spelling}")
            }
        }

        nativeIndex.typedefs.forEach { t ->
            try {
                stubs.add(
                        generateKotlinFragmentBy { generateTypedef(t) }
                )
            } catch (e: Throwable) {
                log("Warning: cannot generate typedef ${t.name}")
            }
        }

        return stubs
    }

    /**
     * Produces to [out] the contents of file with Kotlin bindings.
     */
    private fun generateKotlinFile(stubs: List<StubsFragment>) {
        if (platform == KotlinPlatform.JVM) {
            out("@file:JvmName(${jvmFileClassName.quoteAsKotlinLiteral()})")
        }
        out("@file:Suppress(\"UNUSED_EXPRESSION\", \"UNUSED_VARIABLE\")")
        if (pkgName != "") {
            out("package $pkgName")
            out("")
        }
        if (platform == KotlinPlatform.NATIVE) {
            out("import konan.SymbolName")
        }
        out("import kotlinx.cinterop.*")
        out("")

        stubs.forEach {
            it.kotlinStubLines.forEach { out(it) }
            out("")
        }

        if (platform == KotlinPlatform.JVM) {
            out("private val loadLibrary = System.loadLibrary(\"$libName\")")
        }
    }

    /**
     * Returns the C type to be used for value of given Kotlin type in JNI function implementation.
     */
    fun getCBridgeType(kotlinJniBridgeType: String): String {
        return when (platform) {
            KotlinPlatform.JVM -> getCJniBridgeType(kotlinJniBridgeType)
            KotlinPlatform.NATIVE -> getCNativeBridgeType(kotlinJniBridgeType)
        }
    }

    fun getCNativeBridgeType(kotlinJniBridgeType: String) = when (kotlinJniBridgeType) {
        "Unit" -> "void"
        "Byte" -> "int8_t"
        "Short" -> "int16_t"
        "Int" -> "int32_t"
        "Long" -> "int64_t"
        "Float" -> "float"
        "Double" -> "double" // TODO: float32_t, float64_t?
        "NativePtr" -> "void*"
        else -> TODO(kotlinJniBridgeType)
    }

    /**
     * Returns the C type to be used for value of given Kotlin type in JNI function implementation.
     */
    fun getCJniBridgeType(kotlinJniBridgeType: String) = when (kotlinJniBridgeType) {
        "Unit" -> "void"
        "Byte" -> "jbyte"
        "Short" -> "jshort"
        "Int" -> "jint"
        "Long", "NativePtr" -> "jlong"
        "Float" -> "jfloat"
        "Double" -> "jdouble"
        else -> throw NotImplementedError(kotlinJniBridgeType)
    }

    val libraryForCStubs = configuration.library.copy(
            includes = mutableListOf<String>().apply {
                add("stdint.h")
                if (platform == KotlinPlatform.JVM) {
                    add("jni.h")
                }
                addAll(configuration.library.includes)
            },

            compilerArgs = configuration.library.compilerArgs + when (platform) {
                KotlinPlatform.JVM -> listOf("", "linux", "darwin").map {
                    val javaHome = System.getProperty("java.home")
                    "-I$javaHome/../include/$it"
                }
                KotlinPlatform.NATIVE -> emptyList()
            }
    )

    /**
     * Produces to [out] the contents of C source file to be compiled into JNI lib used for Kotlin bindings impl.
     */
    private fun generateCFile(stubs: List<StubsFragment>, entryPoint: String?) {
        libraryForCStubs.preambleLines.forEach {
            out(it)
        }
        out("")

        stubs.forEach {
            val lines = it.nativeStubLines
            if (lines.isNotEmpty()) {
                lines.forEach {
                    out(it)
                }
                out("")
            }
        }

        if (entryPoint != null) {
            out("extern int Konan_main(int argc, char** argv);")
            out("")
            out("__attribute__((__used__))")
            out("int $entryPoint(int argc, char** argv)  {")
            out("  return Konan_main(argc, argv);")
            out("}")
        }
    }

    private tailrec fun Type.unwrapTypedefs(): Type = if (this is Typedef) {
        this.def.aliased.unwrapTypedefs()
    } else {
        this
    }

    /**
     * Produces to [out] the implementation of JNI function used in Kotlin binding for given C function.
     */
    private fun generateCJniFunction(func: FunctionDecl) {
        if (func.generateAsFfiVarargs()) {
            assert (platform == KotlinPlatform.NATIVE)
            // The C stub simply returns pointer to the function:
            out("void* ${func.cStubName}() { return ${func.name}; }")
            return
        }

        val paramNames = paramNames(func)
        val paramBindings = paramBindings(func)
        val retValBinding = retValBinding(func)

        val parameters = paramBindings
                .map { getCBridgeType(it.kotlinJniBridgeType) }
                .mapIndexed { i, type -> "$type ${paramNames[i]}" }

        val cReturnType = getCBridgeType(retValBinding.kotlinJniBridgeType)

        val funcDecl = when (platform) {
            KotlinPlatform.JVM -> {
                val joinedParameters = if (!parameters.isEmpty()) {
                    parameters.joinToString(separator = ", ", prefix = ", ")
                } else {
                    ""
                }

                val funcFullName = buildString {
                    if (pkgName.isNotEmpty()) {
                        append(pkgName)
                        append('.')
                    }
                    append(jvmFileClassName)
                    append('.')
                    append(func.kotlinExternalName)
                }

                val functionName = "Java_" + funcFullName.replace("_", "_1").replace('.', '_').replace("$", "_00024")
                "JNIEXPORT $cReturnType JNICALL $functionName (JNIEnv *jniEnv, jclass jclss$joinedParameters)"
            }
            KotlinPlatform.NATIVE -> {
                val joinedParameters = parameters.joinToString(", ")
                val functionName = func.cStubName
                "$cReturnType $functionName ($joinedParameters)"
            }
        }

        val arguments = func.parameters.mapIndexed { i, parameter ->
            val cType = parameter.type.getStringRepresentation()
            val name = paramNames[i]
            val unwrappedType = parameter.type.unwrapTypedefs()
            when (unwrappedType) {
                is RecordType -> "*($cType*)$name"
                is ArrayType -> {
                    val pointerCType = PointerType(unwrappedType.elemType).getStringRepresentation()
                    "($pointerCType)$name"
                }
                else -> "($cType)$name"
            }
        }.joinToString(", ")

        val callExpr = "${func.name}($arguments)"

        block(funcDecl) {

            if (cReturnType == "void") {
                out("$callExpr;")
            } else if (func.returnsRecord()) {
                out("*(${func.returnType.getStringRepresentation()}*)retValPlacement = $callExpr;")
                out("return ($cReturnType) retValPlacement;")
            } else {
                out("return ($cReturnType) ($callExpr);")
            }
        }
    }

    fun generateFiles(ktFile: Appendable, cFile: Appendable, entryPoint: String?) {
        val stubs = generateStubs()

        withOutput(cFile) {
            generateCFile(stubs, entryPoint)
        }

        withOutput(ktFile) {
            generateKotlinFile(stubs)
        }
    }
}
