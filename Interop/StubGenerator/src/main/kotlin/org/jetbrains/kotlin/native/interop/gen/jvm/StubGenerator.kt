package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.indexer.*
import java.lang.IllegalStateException

class StubGenerator(
        val nativeIndex: NativeIndex,
        val pkgName: String,
        val libName: String,
        val excludedFunctions: Set<String>,
        val dumpShims: Boolean) {

    /**
     * The names that should not be used for struct classes to prevent name clashes
     */
    val forbiddenStructNames = run {
        val functionNames = nativeIndex.functions.map { it.name }
        val fieldNames = nativeIndex.structs.mapNotNull { it.def }.flatMap { it.fields }.map { it.name }
        (functionNames + fieldNames).toSet()
    }

    /**
     * The name to be used for this struct in Kotlin
     */
    val StructDecl.kotlinName: String
        get() {
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
    val EnumDef.isKotlinEnum: Boolean
            // TODO: if an anonymous enum defines e.g. a function return value or struct field type,
            // then it probably should be represented as Kotlin enum
        get() = !isAnonymous

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

    val EnumType.isKotlinEnum: Boolean
        get() = def.isKotlinEnum

    val EnumType.kotlinName: String
        get() = def.kotlinName


    val functionsToBind = nativeIndex.functions.filter { it.name !in excludedFunctions }

    private val usedFunctionTypes = mutableMapOf<FunctionType, String>()

    val FunctionType.kotlinName: String
        get() {
            return usedFunctionTypes.getOrPut(this, {
                "NativeFunctionType" + (usedFunctionTypes.size + 1)
            })
        }

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

    private fun <R> transaction(action: () -> R): R {
        val lines = mutableListOf<String>()
        val res = withOutput({ lines.add(it) }, action)
        // if action is completed successfully:
        lines.forEach(out)
        return res
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
            is Int8Type -> "char"
            is UInt8Type -> "unsigned char"
            is Int16Type -> "short"
            is UInt16Type -> "unsigned short"
            is Int32Type -> "int"
            is UInt32Type -> "unsigned int"
            is IntPtrType -> "intptr_t"
            is UIntPtrType -> "uintptr_t"
            is Int64Type -> "int64_t"
            is UInt64Type -> "uint64_t"

            is PointerType -> {
                val pointeeType = this.pointeeType
                if (pointeeType is FunctionType) {
                    pointeeType.returnType.getStringRepresentation() + " (*)(" +
                            pointeeType.parameterTypes.map { it.getStringRepresentation() }.joinToString(", ") + ")"
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

            else -> throw kotlin.NotImplementedError()
        }
    }

    val PrimitiveType.kotlinType: String
        get() = when (this) {

            is Int8Type, is UInt8Type -> "Byte"
            is Int16Type, is UInt16Type -> "Short"
            is Int32Type, is UInt32Type -> "Int"
            is IntPtrType, is UIntPtrType, // TODO: 64-bit specific
            is Int64Type, is UInt64Type -> "Long"

            else -> throw NotImplementedError()
        }

    /**
     * Describes the Kotlin native reference type
     *
     * @param typeName the name of the Kotlin native reference type
     * @param typeExpr such Kotlin expression that `typeExpr(ptr)` constructs the reference of this type
     * to the value located at `ptr`
     */
    class NativeRefType(val typeName: String, val typeExpr: String = typeName)

    /**
     * Returns the Kotlin type which describes the reference to value of given (C) type.
     */
    fun getKotlinTypeForRefTo(type: Type): NativeRefType = when (type) {
        is Int8Type, is UInt8Type -> NativeRefType("Int8Box")
        is Int16Type, is UInt16Type -> NativeRefType("Int16Box")
        is Int32Type, is UInt32Type -> NativeRefType("Int32Box")
        is IntPtrType, is UIntPtrType, // TODO: 64-bit specific
        is Int64Type, is UInt64Type -> NativeRefType("Int64Box")

        is RecordType -> NativeRefType("${type.kotlinName}")

        is EnumType -> if (type.isKotlinEnum) {
            NativeRefType("${type.kotlinName}.ref")
        } else {
            getKotlinTypeForRefTo(type.def.baseType)
        }

        is PointerType -> {
            val pointeeType = type.pointeeType
            if (pointeeType is VoidType) {
                NativeRefType("NativePtrBox")
            } else if (pointeeType is FunctionType) {
                NativeRefType("NativeFunctionBox<${getKotlinFunctionType(pointeeType)}>", "${pointeeType.kotlinName}.ref")
            } else {
                val pointeeRefType = getKotlinTypeForRefTo(pointeeType)
                NativeRefType("RefBox<${pointeeRefType.typeName}>", "${pointeeRefType.typeExpr}.ref")
            }
        }

        is ConstArrayType -> {
            val elemRefType = getKotlinTypeForRefTo(type.elemType)
            NativeRefType("NativeArray<${elemRefType.typeName}>", "array[${type.length}](${elemRefType.typeExpr})")
        }

        is IncompleteArrayType -> {
            val elemRefType = getKotlinTypeForRefTo(type.elemType)
            NativeRefType("NativeArray<${elemRefType.typeName}>", "array(${elemRefType.typeExpr})")
        }

        else -> throw NotImplementedError()
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
     * Constructs [OutValueBinding] for the value represented by given native reference type in Kotlin.
     */
    fun outValueRefBinding(refType: NativeRefType) = OutValueBinding(
            kotlinType = refType.typeName + "?",
            kotlinConv = { "$it.getNativePtr().asLong()" },
            kotlinJniBridgeType = "Long"
    )

    /**
     * Constructs [InValueBinding] for the value represented by given native reference type in Kotlin.
     */
    fun inValueRefBinding(refType: NativeRefType) = InValueBinding(
            kotlinJniBridgeType = "Long",
            conv = { "NativePtr.byValue($it).asRef(${refType.typeExpr})" },
            kotlinType = refType.typeName + "?"
    )

    /**
     * Constructs [OutValueBinding] for the value of given C type.
     */
    fun getOutValueBinding(type: Type): OutValueBinding = when (type) {

        is PrimitiveType -> OutValueBinding(type.kotlinType)

        is PointerType -> {
            if (type.pointeeType is VoidType || type.pointeeType is FunctionType) {
                OutValueBinding(
                        kotlinType = "NativePtr?",
                        kotlinConv = { "$it.asLong()" },
                        kotlinJniBridgeType = "Long"
                )
            } else {
                outValueRefBinding(getKotlinTypeForRefTo(type.pointeeType))
            }
        }

        is EnumType -> if (type.isKotlinEnum) {
            OutValueBinding(
                    kotlinType = type.kotlinName,
                    kotlinConv = { "$it.value" },
                    kotlinJniBridgeType = type.def.baseType.kotlinType
            )
        } else {
            getOutValueBinding(type.def.baseType)
        }

        is ArrayType -> outValueRefBinding(getKotlinTypeForRefTo(type))

        else -> throw NotImplementedError()
    }

    fun getCFunctionParamBinding(type: Type): OutValueBinding {
        when (type) {
            is PointerType -> {
                val pointeeType = type.pointeeType
                when (pointeeType) {
                    is FunctionType -> return OutValueBinding(
                            kotlinType = "(" + getKotlinFunctionType(pointeeType) + ")?",
                            kotlinConv = { "$it?.staticAsNative(${pointeeType.kotlinName}).asLong()" },
                            kotlinJniBridgeType = "Long"
                    )
                    is Int8Type -> return OutValueBinding(
                            kotlinType = "String?",
                            kotlinConv = { name -> "$name?.toCString(memScope).getNativePtr().asLong()" },
                            memScoped = true,
                            kotlinJniBridgeType = "Long"
                    )
                }
            }
            is RecordType -> {
                val refType = getKotlinTypeForRefTo(type)
                // pointer will be converted to value in C code
                return OutValueBinding(
                        kotlinType = refType.typeName,
                        kotlinConv = { "$it.getNativePtr().asLong()" },
                        kotlinJniBridgeType = "Long"
                )
            }
        }

        return getOutValueBinding(type)
    }

    fun getCallbackRetValBinding(type: Type): OutValueBinding {
        if (type is VoidType) {
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
    fun getInValueBinding(type: Type): InValueBinding = when (type) {

        is PrimitiveType -> InValueBinding(type.kotlinType)

        is PointerType -> {
            if (type.pointeeType is VoidType || type.pointeeType is FunctionType) {
                InValueBinding(
                        kotlinJniBridgeType = "Long",
                        conv = { "NativePtr.byValue($it)" },
                        kotlinType = "NativePtr?"
                )
            } else {
                inValueRefBinding(getKotlinTypeForRefTo(type.pointeeType))
            }
        }

        is EnumType -> if (type.isKotlinEnum) {
            InValueBinding(
                    kotlinJniBridgeType = type.def.baseType.kotlinType,
                    conv = { "${type.kotlinName}.byValue($it)" },
                    kotlinType = type.kotlinName
            )
        } else {
            getInValueBinding(type.def.baseType)
        }

        is ArrayType -> inValueRefBinding(getKotlinTypeForRefTo(type))

        else -> throw NotImplementedError()
    }

    fun getCFunctionRetValBinding(type: Type): InValueBinding {
        when (type) {
            is VoidType -> return InValueBinding("Unit")

            is RecordType -> {
                val refType = getKotlinTypeForRefTo(type)
                return InValueBinding(
                        kotlinJniBridgeType = "Long",
                        conv = { "NativePtr.byValue($it).asRef(${refType.typeExpr})!!" },
                        kotlinType = refType.typeName
                )
            }
        }

        return getInValueBinding(type)
    }

    fun getCallbackParamBinding(type: Type): InValueBinding {
        when (type) {
            is RecordType -> {
                val refType = getKotlinTypeForRefTo(type)
                return InValueBinding(
                        kotlinJniBridgeType = "Long",
                        conv = { throw UnsupportedOperationException() },
                        kotlinType = refType.typeName
                )
            }
        }
        return getInValueBinding(type)
    }

    private fun getKotlinFunctionType(type: FunctionType): String {
        return "(" +
                type.parameterTypes.map { getCallbackParamBinding(it).kotlinType }.joinToString(", ") +
                ") -> " +
                getCallbackRetValBinding(type.returnType).kotlinType
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

        val className = decl.kotlinName
        block("class $className(ptr: NativePtr) : NativeStruct(ptr)") {
            out("")
            out("companion object : Type<$className>(${def.size}, ::$className)")
            out("")
            def.fields.forEach { field ->
                try {
                    if (field.offset < 0) throw NotImplementedError();
                    assert(field.offset % 8 == 0L)
                    val offset = field.offset / 8
                    val fieldRefType = getKotlinTypeForRefTo(field.type)
                    out("val ${field.name} by ${fieldRefType.typeExpr} at $offset")
                } catch (e: Throwable) {
                    println("Warning: cannot generate definition for field $className.${field.name}")
                }
            }
        }
    }

    /**
     * Produces to [out] the definition of Kotlin class representing the reference to given forward (incomplete) struct.
     */
    private fun generateForwardStruct(s: StructDecl) {
        val className = s.kotlinName
        block("class $className(ptr: NativePtr) : NativeRef(ptr)") {
            out("companion object : Type<$className>(::$className)")
        }
    }

    /**
     * Produces to [out] the Kotlin definitions for given enum.
     */
    private fun generateEnum(e: EnumDef) {
        if (!e.isKotlinEnum) {
            generateEnumAsValues(e)
            return
        }

        val baseRefType = getKotlinTypeForRefTo(e.baseType)

        block("enum class ${e.kotlinName}(val value: ${e.baseType.kotlinType})") {
            e.values.forEach {
                out("${it.name}(${it.value}),")
            }
            out(";")
            out("")
            block("companion object") {
                out("fun byValue(value: ${e.baseType.kotlinType}) = ${e.kotlinName}.values().find { it.value == value }!!")
            }
            out("")
            block("class ref(ptr: NativePtr) : NativeRef(ptr)") {
                out("companion object : TypeWithSize<ref>(${baseRefType.typeExpr}.size, ::ref)")
                out("var value: ${e.kotlinName}")
                out("    get() = byValue(${baseRefType.typeExpr}.byPtr(ptr).value)")
                out("    set(value) { ${baseRefType.typeExpr}.byPtr(ptr).value = value.value }")
            }
        }
    }

    /**
     * Produces to [out] the Kotlin definitions for given enum which shouldn't be represented as Kotlin enum.
     *
     * @see isKotlinEnum
     */
    private fun generateEnumAsValues(e: EnumDef) {
        // TODO: if this enum defines e.g. a type of struct field, then it should be generated inside the struct class
        // to prevent name clashing

        if (e.values.isEmpty()) {
            return
        }

        out("// ${e.spelling}")
        e.values.forEach {
            out("val ${it.name}: ${e.baseType.kotlinType} = ${it.value}")
        }
    }

    /**
     * Constructs [InValueBinding] for return value of Kotlin binding for given C function.
     */
    private fun retValBinding(func: FunctionDecl) = getCFunctionRetValBinding(func.returnType)

    /**
     * Constructs [OutValueBinding]s for parameters of Kotlin binding for given C function.
     */
    private fun paramBindings(func: FunctionDecl): Array<OutValueBinding> {
        val paramBindings = func.parameters.map { param ->
            getCFunctionParamBinding(param.type)
        }.toMutableList()

        val retValType = func.returnType
        if (retValType is RecordType) {
            val retValRefType = getKotlinTypeForRefTo(retValType)
            val typeExpr = retValRefType.typeExpr

            paramBindings.add(OutValueBinding(
                    kotlinType = "Placement",
                    kotlinConv = { name -> "$name.alloc($typeExpr.size).asLong()" },
                    kotlinJniBridgeType = "Long"
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

        if (func.returnType is RecordType) {
            paramNames.add("retValPlacement")
        }

        return paramNames.toTypedArray()
    }

    /**
     * Produces to [out] the definition of Kotlin binding for given C function.
     */
    private fun generateKotlinBindingMethod(func: FunctionDecl) {
        val paramNames = paramNames(func)
        val paramBindings = paramBindings(func)
        val retValBinding = retValBinding(func)

        val args = paramNames.mapIndexed { i: Int, name: String ->
            "$name: " + paramBindings[i].kotlinType
        }.joinToString(", ")

        val header = "fun ${func.name}($args): ${retValBinding.kotlinType}"

        fun generateBody() {
            val externalParamNames = paramNames.mapIndexed { i: Int, name: String ->
                val binding = paramBindings[i]
                val externalParamName: String

                if (binding.kotlinConv != null) {
                    externalParamName = "_$name"
                    out("val $externalParamName = " + binding.kotlinConv.invoke(name))
                } else {
                    externalParamName = name
                }

                externalParamName

            }
            out("val res = externals.${func.name}(" + externalParamNames.joinToString(", ") + ")")

            if (dumpShims) {
                val returnValueRepresentation  = retValBinding.conv("res")
                out("print(\"\${${returnValueRepresentation}}\\t= \")")
                out("print(\"${func.name}( \")")
                val argsRepresentation = paramNames.map{"\${${it}}"}.joinToString(", ")
                out("print(\"${argsRepresentation}\")")
                out("println(\")\")")
            }

            out("return " + retValBinding.conv("res"))
        }

        block(header) {
            val memScoped = paramBindings.any { it.memScoped }
            if (memScoped) {
                block("memScoped") {
                    generateBody()
                }
            } else {
                generateBody()
            }
        }
    }

    private fun getFfiStructType(elementTypes: List<Type>) =
            "Struct(" +
                    elementTypes.map { getFfiType(it) }.joinToString(", ") +
                    ")"

    private fun getFfiType(type: Type): String {
        return when(type) {
            is VoidType -> "Void"
            is Int8Type -> "SInt8"
            is UInt8Type -> "UInt8"
            is Int16Type -> "SInt16"
            is UInt16Type -> "UInt16"
            is Int32Type -> "SInt32"
            is UInt32Type -> "UInt32"
            is IntPtrType, is UIntPtrType, // TODO
            is PointerType -> "Pointer"
            is ConstArrayType -> getFfiStructType(
                    Array(type.length.toInt(), { type.elemType }).toList()
            )
            is EnumType -> getFfiType(type.def.baseType)
            is RecordType -> {
                val def = type.decl.def!!
                if (!def.hasNaturalLayout) {
                    throw NotImplementedError() // TODO: represent pointer to function as NativePtr instead
                }
                getFfiStructType(def.fields.map { it.type })
            }
            else -> throw NotImplementedError(type.toString())
        }
    }

    private fun getArgFfiType(type: Type) = when (type) {
        is ArrayType -> "Pointer"
        else -> getFfiType(type)
    }

    private fun getRetValFfiType(type: Type) = getArgFfiType(type)

    private fun generateFunctionType(type: FunctionType, name: String) {
        val kotlinFunctionType = getKotlinFunctionType(type)

        val constructorArgs = listOf(getRetValFfiType(type.returnType)) +
                type.parameterTypes.map { getArgFfiType(it) }

        val constructorArgsStr = constructorArgs.joinToString(", ")

        block("object $name : NativeFunctionType<$kotlinFunctionType>($constructorArgsStr)") {
            block("override fun invoke(function: $kotlinFunctionType, args: NativeArray<NativePtrBox>, ret: NativePtr)") {
                val args = type.parameterTypes.mapIndexed { i, paramType ->
                    val refType = getKotlinTypeForRefTo(paramType)
                    val ref = "args[$i].value.asRef(${refType.typeExpr})!!"
                    when (paramType) {
                        is RecordType -> "$ref"
                        else -> "$ref.value"
                    }
                }.joinToString(", ")

                out("val res = function($args)")

                when (type.returnType) {
                    is RecordType -> throw NotImplementedError()
                    is VoidType -> {} // nothing to do
                    else -> {
                        val retRefType = getKotlinTypeForRefTo(type.returnType)
                        out("${retRefType.typeExpr}.byPtr(ret).value = res")
                    }
                }

            }
        }
    }

    /**
     * Produces to [out] the definition of Kotlin JNI function used in binding for given C function.
     */
    private fun generateKotlinExternalMethod(func: FunctionDecl) {
        val paramNames = paramNames(func)
        val paramBindings = paramBindings(func)
        val retValBinding = retValBinding(func)

        val args = paramNames.mapIndexed { i: Int, name: String ->
            "$name: " + paramBindings[i].kotlinJniBridgeType
        }.joinToString(", ")

        out("external fun ${func.name}($args): ${retValBinding.kotlinJniBridgeType}")
    }

    /**
     * Produces to [out] the contents of file with Kotlin bindings.
     */
    fun generateKotlinFile() {
        if (pkgName != "") {
            out("package $pkgName")
            out("")
        }
        out("import kotlin_native.interop.*")
        out("")

        functionsToBind.forEach {
            try {
                transaction {
                    generateKotlinBindingMethod(it)
                    out("")
                }
            } catch (e: Throwable) {
                println("Warning: cannot generate binding definition for function ${it.name}")
            }
        }

        nativeIndex.structs.forEach { s ->
            try {
                transaction {
                    generateStruct(s)
                    out("")
                }
            } catch (e: Throwable) {
                println("Warning: cannot generate definition for struct ${s.kotlinName}")
            }
        }

        nativeIndex.enums.forEach { e ->
            generateEnum(e)
            out("")
        }

        usedFunctionTypes.entries.forEach {
            generateFunctionType(it.key, it.value)
            out("")
        }

        block("object externals") {
            out("init { System.loadLibrary(\"$libName\") }")
            functionsToBind.forEach {
                try {
                    transaction {
                        generateKotlinExternalMethod(it)
                        out("")
                    }
                } catch (e: Throwable) {
                    println("Warning: cannot generate external definition for function ${it.name}")
                }
            }
        }
    }

    /**
     * Returns the C type to be used for value of given Kotlin type in JNI function implementation.
     */
    fun getCJniBridgeType(kotlinJniBridgeType: String) = when (kotlinJniBridgeType) {
        "Unit" -> "void"
        "Byte" -> "jbyte"
        "Short" -> "jshort"
        "Int" -> "jint"
        "Long" -> "jlong"
        else -> throw NotImplementedError(kotlinJniBridgeType)
    }

    /**
     * Produces to [out] the contents of C source file to be compiled into JNI lib used for Kotlin bindings impl.
     */
    fun generateCFile(headerFiles: List<String>) {
        out("#include <stdint.h>")
        out("#include <jni.h>")
        headerFiles.forEach {
            out("#include <$it>")
        }
        out("")

        functionsToBind.forEach { func ->
            try {
                generateCJniFunction(func)
            } catch (e: Throwable) {
                System.err.println("Warning: cannot generate C JNI function definition ${func.name}")
            }
        }
    }

    /**
     * Produces to [out] the implementation of JNI function used in Kotlin binding for given C function.
     */
    private fun generateCJniFunction(func: FunctionDecl) {
        val funcReturnType = func.returnType
        val paramNames = paramNames(func)
        val paramBindings = paramBindings(func)
        val retValBinding = retValBinding(func)

        val args =
                if (paramBindings.isEmpty())
                    ""
                else paramBindings
                        .map { getCJniBridgeType(it.kotlinJniBridgeType) }
                        .mapIndexed { i, type -> "$type ${paramNames[i]}" }
                        .joinToString(separator = ", ", prefix = ", ")

        val cReturnType = getCJniBridgeType(retValBinding.kotlinJniBridgeType)

        val params = func.parameters.mapIndexed { i, parameter ->
            val cType = parameter.type.getStringRepresentation()
            if (parameter.type is RecordType) {
                "*($cType*)${paramNames[i]}"
            } else {
                "($cType)${paramNames[i]}"
            }
        }.joinToString(", ")

        val callExpr = "${func.name}($params)"
        val funcFullName = if (pkgName.isEmpty()) {
            "externals.${func.name}"
        } else {
            "$pkgName.externals.${func.name}"
        }
        val jniFuncName = "Java_" + funcFullName.replace("_", "_1").replace('.', '_').replace("$", "_00024")

        block("JNIEXPORT $cReturnType JNICALL $jniFuncName (JNIEnv *env, jobject obj$args)") {

            if (cReturnType == "void") {
                out("$callExpr;")
            } else if (funcReturnType is RecordType) {
                out("*(${funcReturnType.decl.spelling}*)retValPlacement = $callExpr;")
                out("return ($cReturnType) retValPlacement;")
            } else {
                out("return ($cReturnType) ($callExpr);")
            }
        }
    }
}
