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

import org.jetbrains.kotlin.native.interop.gen.*
import org.jetbrains.kotlin.native.interop.indexer.*
import java.lang.IllegalStateException

enum class KotlinPlatform {
    JVM,
    NATIVE
}

class StubGenerator(
        val nativeIndex: NativeIndex,
        val configuration: InteropConfiguration,
        val libName: String,
        val dumpShims: Boolean,
        val verbose: Boolean = false,
        val platform: KotlinPlatform = KotlinPlatform.JVM) {

    private var theCounter = 0
    fun nextUniqueId() = theCounter++

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

    val platformWStringTypes = setOf("LPCWSTR")

    /**
     * The names that should not be used for struct classes to prevent name clashes
     */
    val forbiddenStructNames = run {
        val functionNames = nativeIndex.functions.map { it.name }
        val fieldNames = nativeIndex.structs.mapNotNull { it.def }.flatMap { it.fields }.map { it.name }
        val typedefNames = nativeIndex.typedefs.map { it.name }
        (functionNames + fieldNames + typedefNames).toSet()
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

    val declarationMapper = object : DeclarationMapper {
        override fun getKotlinNameForPointed(structDecl: StructDecl): String = structDecl.kotlinName

        override fun isMappedToStrict(enumDef: EnumDef): Boolean = enumDef.isStrictEnum

        override fun getKotlinNameForValue(enumDef: EnumDef): String = enumDef.kotlinName
    }

    fun mirror(type: Type): TypeMirror = mirror(declarationMapper, type)

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

    private fun generateKotlinFragmentBy(block: () -> Unit): KotlinStub {
        val lines = generateLinesBy(block)
        return object : KotlinStub {
            override fun generate(context: StubGenerationContext) = lines.asSequence()
        }
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

    private fun Type.isAliasOf(names: Set<String>): Boolean {
        var type = this
        while (type is Typedef) {
            if (names.contains(type.def.name)) return true
            type = type.def.aliased
        }
        return false
    }

    fun representCFunctionParameterAsString(type: Type): Boolean {
        val unwrappedType = type.unwrapTypedefs()
        return unwrappedType is PointerType && unwrappedType.pointeeIsConst &&
                unwrappedType.pointeeType.unwrapTypedefs() == CharType
    }

    // We take this approach as generic 'const short*' shall not be used as String.
    fun representCFunctionParameterAsWString(type: Type)= type.isAliasOf(platformWStringTypes)

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

        block("class ${decl.kotlinName.asSimpleName()}(rawPtr: NativePtr) : CStructVar(rawPtr)") {
            out("")
            out("companion object : Type(${def.size}, ${def.align})") // FIXME: align
            out("")
            for (field in def.fields) {
                try {
                    assert(field.name.isNotEmpty())

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

            if (platform == KotlinPlatform.NATIVE) {
                for (field in def.bitFields) {
                    val typeMirror = mirror(field.type)
                    val typeInfo = typeMirror.info
                    val kotlinType = typeMirror.argType
                    val rawType = typeInfo.bridgedType

                    out("var ${field.name.asSimpleName()}: $kotlinType")

                    val signed = field.type.getUnderlyingIntegerType().isSigned

                    val readBitsExpr =
                            "readBits(this.rawPtr, ${field.offset}, ${field.size}, $signed).${rawType.convertor!!}()"

                    out("    get() = ${typeInfo.argFromBridged(readBitsExpr)}")

                    val rawValue = typeInfo.argToBridged("value")
                    val setExpr = "writeBits(this.rawPtr, ${field.offset}, ${field.size}, $rawValue.toLong())"
                    out("    set(value) = $setExpr")
                    out("")
                }
            }
        }
    }

    private tailrec fun Type.getUnderlyingIntegerType(): IntegerType = when (this) {
        is IntegerType -> this
        is EnumType -> this.def.baseType.getUnderlyingIntegerType()
        is Typedef -> this.def.aliased.getUnderlyingIntegerType()
        else -> error(this)
    }

    /**
     * Produces to [out] the definition of Kotlin class representing the reference to given forward (incomplete) struct.
     */
    private fun generateForwardStruct(s: StructDecl) {
        out("class ${s.kotlinName.asSimpleName()}(rawPtr: NativePtr) : COpaque(rawPtr)")
    }

    private fun EnumConstant.isMoreCanonicalThan(other: EnumConstant): Boolean = with(other.name.toLowerCase()) {
        contains("min") || contains("max") ||
                contains("first") || contains("last") ||
                contains("begin") || contains("end")
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
        val baseKotlinType = baseTypeMirror.argType

        val canonicalsByValue = e.constants
                .groupingBy { it.value }
                .reduce { _, accumulator, element ->
                    if (element.isMoreCanonicalThan(accumulator)) {
                        element
                    } else {
                        accumulator
                    }
                }

        val (canonicalConstants, aliasConstants) = e.constants.partition { canonicalsByValue[it.value] == it }

        block("enum class ${e.kotlinName.asSimpleName()}(override val value: $baseKotlinType) : CEnum") {
            canonicalConstants.forEach {
                out("${it.name.asSimpleName()}(${it.value}),")
            }
            out(";")
            out("")
            block("companion object") {
                aliasConstants.forEach {
                    val mainConstant = canonicalsByValue[it.value]!!
                    out("val ${it.name.asSimpleName()} = ${mainConstant.name.asSimpleName()}")
                }
                if (aliasConstants.isNotEmpty()) out("")

                out("fun byValue(value: $baseKotlinType) = " +
                        "${e.kotlinName.asSimpleName()}.values().find { it.value == value }!!")
            }
            out("")
            block("class Var(rawPtr: NativePtr) : CEnumVar(rawPtr)") {
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

        val baseKotlinType = mirror(e.baseType).argType
        if (e.isAnonymous) {
            if (constants.isNotEmpty()) {
                out("// ${e.spelling}:")
            }

            typeName = baseKotlinType
        } else {
            val typeMirror = mirror(EnumType(e))
            if (typeMirror !is TypeMirror.ByValue) {
                error("unexpected enum type mirror: $typeMirror")
            }

            // Generate as typedef:
            val varTypeName = typeMirror.info.constructPointedType(typeMirror.valueTypeName)
            out("typealias ${typeMirror.pointedTypeName} = $varTypeName")
            out("typealias ${typeMirror.valueTypeName} = $baseKotlinType")

            if (constants.isNotEmpty()) {
                out("")
            }

            typeName = typeMirror.valueTypeName
        }

        for (constant in constants) {
            val literal = integerLiteral(e.baseType, constant.value) ?: continue
            out("val ${constant.name.asSimpleName()}: $typeName = $literal")
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

    private fun generateStubsForFunctions(functions: List<FunctionDecl>): List<KotlinStub> {
        val stubs = functions.mapNotNull {
            try {
                KotlinFunctionStub(it)
            } catch (e: Throwable) {
                log("Warning: cannot generate stubs for function ${it.name}")
                null
            }
        }

        return stubs
    }

    private fun FunctionDecl.generateAsFfiVarargs(): Boolean = (platform == KotlinPlatform.NATIVE && this.isVararg &&
            // Neither takes nor returns structs by value:
            !this.returnsRecord() && this.parameters.all { it.type.unwrapTypedefs() !is RecordType })

    private fun FunctionDecl.returnsRecord(): Boolean = this.returnType.unwrapTypedefs() is RecordType
    private fun FunctionDecl.returnsVoid(): Boolean = this.returnType.unwrapTypedefs() is VoidType

    private inner class KotlinFunctionStub(val func: FunctionDecl) : KotlinStub, NativeBacked {
        override fun generate(context: StubGenerationContext): Sequence<String> =
                if (context.nativeBridges.isSupported(this)) {
                    block(header, bodyLines)
                } else {
                    sequenceOf(
                            annotationForUnableToImport,
                            "external $header"
                    )
                }

        private val header: String
        private val bodyLines: List<String>

        init {
            // TODO: support dumpShims
            val kotlinParameters = mutableListOf<Pair<String, String>>()
            val bodyGenerator = KotlinCodeBuilder()
            val bridgeArguments = mutableListOf<TypedKotlinValue>()

            func.parameters.forEachIndexed { index, parameter ->
                val parameterName = parameter.name.let {
                    if (it == null || it.isEmpty()) {
                        "arg$index"
                    } else {
                        it.asSimpleName()
                    }
                }

                val tmpVarName = "kniTmp$index"

                val representAsValuesRef = representCFunctionParameterAsValuesRef(parameter.type)

                val bridgeArgument = if (representCFunctionParameterAsString(parameter.type)) {
                    kotlinParameters.add(parameterName to "String?")
                    bodyGenerator.pushBlock("$parameterName?.cstr.usePointer { $tmpVarName ->")
                    tmpVarName
                } else if (representCFunctionParameterAsWString(parameter.type)) {
                    kotlinParameters.add(parameterName to "String?")
                    bodyGenerator.pushBlock("$parameterName?.wcstr.usePointer { $tmpVarName ->")
                    tmpVarName
                } else if (representAsValuesRef != null) {
                    kotlinParameters.add(parameterName to "CValuesRef<${mirror(representAsValuesRef).pointedTypeName}>?")
                    bodyGenerator.pushBlock("$parameterName.usePointer { $tmpVarName ->")
                    tmpVarName
                } else {
                    val mirror = mirror(parameter.type)
                    kotlinParameters.add(parameterName to mirror.argType)
                    parameterName
                }

                bridgeArguments.add(TypedKotlinValue(parameter.type, bridgeArgument))
            }

            if (!func.generateAsFfiVarargs()) {
                val result = mappingBridgeGenerator.kotlinToNative(
                        bodyGenerator,
                        this,
                        func.returnType,
                        bridgeArguments
                ) { nativeValues ->
                    "${func.name}(${nativeValues.joinToString()})"
                }
                bodyGenerator.out("return $result")
            } else {
                val returnTypeKind = getFfiTypeKind(func.returnType)

                kotlinParameters.add("vararg variadicArguments" to "Any?")
                bodyGenerator.pushBlock("memScoped {")

                val resultVar = "kniResult"

                val resultPtr = if (!func.returnsVoid()) {
                    val returnType = mirror(func.returnType).pointedTypeName
                    bodyGenerator.out("val $resultVar = allocFfiReturnValueBuffer<$returnType>(typeOf<$returnType>())")
                    "$resultVar.rawPtr"
                } else {
                    "nativeNullPtr"
                }
                val fixedArguments = bridgeArguments.joinToString(", ") { it.value }

                val functionPtr = simpleBridgeGenerator.kotlinToNative(
                        this,
                        BridgedType.NATIVE_PTR,
                        emptyList()
                ) {
                    func.name
                }

                bodyGenerator.out("callWithVarargs($functionPtr, $resultPtr, $returnTypeKind, " +
                        "arrayOf($fixedArguments), variadicArguments, memScope)")

                if (!func.returnsVoid()) {
                    bodyGenerator.out("return $resultVar.value")
                }
            }

            val returnType = if (func.returnsVoid()) {
                "Unit"
            } else {
                mirror(func.returnType).argType
            }

            val joinedKotlinParameters = kotlinParameters.joinToString { (name, type) -> "$name: $type" }
            this.header = "fun ${func.name}($joinedKotlinParameters): $returnType"

            this.bodyLines = bodyGenerator.build()
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

    private fun integerLiteral(type: Type, value: Long): String? {
        if (value == Long.MIN_VALUE) {
            return "${value + 1} - 1" // Workaround for "The value is out of range" compile error.
        }

        val unwrappedType = type.unwrapTypedefs()
        if (unwrappedType !is PrimitiveType) {
            return null
        }

        val narrowedValue: Number = when (unwrappedType.kotlinType) {
            "kotlin.Byte" -> value.toByte()
            "kotlin.Short" -> value.toShort()
            "kotlin.Int" -> value.toInt()
            "kotlin.Long" -> value
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

    private fun generateStubs(): List<KotlinStub> {
        val stubs = mutableListOf<KotlinStub>()

        stubs.addAll(generateStubsForFunctions(functionsToBind))

        nativeIndex.objCProtocols.mapTo(stubs) {
            ObjCProtocolStub(this, it)
        }

        nativeIndex.objCClasses.mapTo(stubs) {
            ObjCClassStub(this, it)
        }

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
    private fun generateKotlinFile(nativeBridges: NativeBridges, stubs: List<KotlinStub>) {
        if (platform == KotlinPlatform.JVM) {
            out("@file:JvmName(${jvmFileClassName.quoteAsKotlinLiteral()})")
        }
        if (platform == KotlinPlatform.NATIVE) {
            out("@file:kotlinx.cinterop.InteropStubs")
        }

        val suppress = mutableListOf("UNUSED_VARIABLE", "UNUSED_EXPRESSION").apply {
            if (configuration.library.language == Language.OBJECTIVE_C) {
                add("CONFLICTING_OVERLOADS")
                add("RETURN_TYPE_MISMATCH_ON_INHERITANCE")
                add("RETURN_TYPE_MISMATCH_ON_OVERRIDE")
                add("WRONG_MODIFIER_CONTAINING_DECLARATION") // For `final val` in interface.
                add("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                add("UNUSED_PARAMETER") // For constructors.
            }
        }

        out("@file:Suppress(${suppress.joinToString { it.quoteAsKotlinLiteral() }})")
        if (pkgName != "") {
            out("package $pkgName")
            out("")
        }
        if (platform == KotlinPlatform.NATIVE) {
            out("import konan.SymbolName")
        }
        out("import kotlinx.cinterop.*")
        out("")

        val context = object : StubGenerationContext {
            val topLevelDeclarationLines = mutableListOf<String>()

            override val nativeBridges: NativeBridges get() = nativeBridges
            override fun addTopLevelDeclaration(lines: List<String>) {
                topLevelDeclarationLines.addAll(lines)
            }
        }

        stubs.forEach {
            it.generate(context).forEach(out)
            out("")
        }

        context.topLevelDeclarationLines.forEach(out)
        nativeBridges.kotlinLines.forEach(out)
        if (platform == KotlinPlatform.JVM) {
            out("private val loadLibrary = System.loadLibrary(\"$libName\")")
        }
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
                KotlinPlatform.JVM -> listOf("", "linux", "darwin", "win32").map {
                    val javaHome = System.getProperty("java.home")
                    "-I$javaHome/../include/$it"
                }
                KotlinPlatform.NATIVE -> emptyList()
            }
    )

    /**
     * Produces to [out] the contents of C source file to be compiled into native lib used for Kotlin bindings impl.
     */
    private fun generateCFile(bridges: NativeBridges, entryPoint: String?) {
        libraryForCStubs.preambleLines.forEach {
            out(it)
        }
        out("")

        bridges.nativeLines.forEach {
            out(it)
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

    fun generateFiles(ktFile: Appendable, cFile: Appendable, entryPoint: String?) {
        val stubs = generateStubs()

        val nativeBridges = simpleBridgeGenerator.prepare()

        withOutput(cFile) {
            generateCFile(nativeBridges, entryPoint)
        }

        withOutput(ktFile) {
            generateKotlinFile(nativeBridges, stubs)
        }
    }

    val simpleBridgeGenerator: SimpleBridgeGenerator =
            SimpleBridgeGeneratorImpl(platform, pkgName, jvmFileClassName, libraryForCStubs)

    val mappingBridgeGenerator: MappingBridgeGenerator =
            MappingBridgeGeneratorImpl(declarationMapper, simpleBridgeGenerator)

}
