package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kni.indexer.NativeIndex
import org.jetbrains.kotlin.native.interop.gen.*

class StubGenerator(
        val translationUnit: NativeIndex.TranslationUnit,
        val pkgName: String,
        val libName: String,
        val excludedFunctions: Set<String>) {

    val forbiddenStructNames = run {
        val functionNames = translationUnit.functionList.map { it.name }
        val fieldNames = translationUnit.structList.flatMap { it.fieldList }.map { it.name }
        (functionNames + fieldNames).toSet()
    }

    val enums = translationUnit.enumList.map { it.name to it }.toMap()

    val functionsToBind = translationUnit.functionList.uniqueBy { it.name }.filter { it.name !in excludedFunctions }

    private fun mangleStructName(name: String) = if (name !in forbiddenStructNames) name else (name + "Struct")

    val NativeIndex.CStruct.mangledName: String
        get() = mangleStructName(name)


    val NativeIndex.CForwardStruct.mangledName: String
        get() = mangleStructName(name)

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

    fun CType.getStringRepresentation(): String {
        return when (this) {
            is VoidType -> "void"
            is Int8Type -> "char"
            is UInt8Type -> "unsigned char"
            is Int16Type -> "short"
            is UInt16Type -> "unsigned short"
            is Int32Type -> "int"
            is UInt32Type -> "unsigned int"
            is Int64Type -> "int64_t"
            is UInt64Type -> "uint64_t"
            is PointerType -> pointeeType.getStringRepresentation() + "*"
            is RecordType -> "struct $name"
            is FunctionPointerType -> this.returnType.getStringRepresentation() + " (*)(" +
                    this.parameterTypes.map { it.getStringRepresentation() }.joinToString(", ") + ")"
            is ArrayType -> "void*" // TODO
            is EnumType -> enums["${this.name}"]!!.spelling
            else -> throw kotlin.NotImplementedError()
        }
    }

    class NativeRefType(val typeName: String, val typeExpr: String = typeName)

    fun getKotlinNativeRefType(type: CType): NativeRefType = when (type) {
        is Int8Type, is UInt8Type -> NativeRefType("Int8Box")
        is Int16Type, is UInt16Type -> NativeRefType("Int16Box")
        is Int32Type, is UInt32Type -> NativeRefType("Int32Box")
        is Int64Type, is UInt64Type -> NativeRefType("Int64Box")
        is RecordType -> NativeRefType("${mangleStructName(type.name)}")
        is PointerType -> {
            if (type.pointeeType is VoidType) {
                NativeRefType("NativePtrBox")
            } else {
                val pointeeRefType = getKotlinNativeRefType(type.pointeeType)
                NativeRefType("RefBox<${pointeeRefType.typeName}>", "${pointeeRefType.typeExpr}.ref")
            }
        }
        is FunctionPointerType -> getKotlinNativeRefType(PointerType(VoidType))
        is ConstArrayType -> {
            val elemRefType = getKotlinNativeRefType(type.elemType)
            NativeRefType("NativeArray<${elemRefType.typeName}>", "array[${type.length}](${elemRefType.typeExpr})")
        }
        is IncompleteArrayType -> {
            val elemRefType = getKotlinNativeRefType(type.elemType)
            NativeRefType("NativeArray<${elemRefType.typeName}>", "array(${elemRefType.typeExpr})")
        }
        else -> throw NotImplementedError()
    }

    class OutValueBinding(val kotlinType: String,
                          val kotlinConv: ((String) -> String)? = null,
                          val convFree: ((String) -> String)? = null,
                          val kotlinJniBridgeType: String = kotlinType)

    class InValueBinding(val kotlinJniBridgeType: String,
                         val conv: ((String) -> String) = { it },
                         val kotlinType: String = kotlinJniBridgeType)

    fun outValueRefBinding(refType: NativeRefType) = OutValueBinding(
            kotlinType = refType.typeName + "?",
            kotlinConv = { "$it.getNativePtr().asLong()" },
            kotlinJniBridgeType = "Long"
    )

    fun inValueRefBinding(refType: NativeRefType) = InValueBinding(
            kotlinJniBridgeType = "Long",
            conv = { "NativePtr.byValue($it).asRef(${refType.typeExpr})" },
            kotlinType = refType.typeName + "?"
    )

    fun getOutValueBinding(type: CType): OutValueBinding = when (type) {
        is DirectlyMappedType -> OutValueBinding(type.kotlinType)
        is PointerType -> {
            if (type.pointeeType is VoidType) {
                OutValueBinding(
                        kotlinType = "NativePtr?",
                        kotlinConv = { "$it.asLong()" },
                        kotlinJniBridgeType = "Long"
                )
            } else if (type.pointeeType is Int8Type) {
                OutValueBinding(
                        kotlinType = "String?",
                        kotlinConv = { name -> "CString.fromString($name).getNativePtr().asLong()" },
                        convFree = { name -> "free(NativePtr.byValue($name))" },
                        kotlinJniBridgeType = "Long"
                )
            } else {
                outValueRefBinding(getKotlinNativeRefType(type.pointeeType))
            }
        }
        is EnumType -> OutValueBinding(
                kotlinType = type.name,
                kotlinConv = { "$it.value" },
                kotlinJniBridgeType = "Long"
        )
        is ArrayType -> outValueRefBinding(getKotlinNativeRefType(type))
        is FunctionPointerType -> getOutValueBinding(PointerType(VoidType))
        else -> throw NotImplementedError()
    }

    fun getInValueBinding(type: CType): InValueBinding = when (type) {
        is VoidType -> InValueBinding("Unit")
        is DirectlyMappedType -> InValueBinding(type.kotlinType)
        is PointerType -> {
            if (type.pointeeType is VoidType) {
                InValueBinding(
                        kotlinJniBridgeType = "Long",
                        conv = { "NativePtr.byValue($it)" },
                        kotlinType = "NativePtr?"
                )
            } else {
                inValueRefBinding(getKotlinNativeRefType(type.pointeeType))
            }
        }
        is EnumType -> InValueBinding(
                kotlinJniBridgeType = "Long",
                conv = { "${type.name}.byValue($it)" },
                kotlinType = type.name
        )
        is ArrayType -> inValueRefBinding(getKotlinNativeRefType(type))
        else -> throw NotImplementedError()
    }


    private fun <T> Iterable<T>.uniqueBy(key: (T) -> Any): List<T> {
        val found = mutableSetOf<Any>()
        return this.filter { found.add(key(it)) }
    }


    private fun generateKotlinStruct(s: NativeIndex.CStruct) {
        val className = s.mangledName
        out("class $className(ptr: NativePtr) : NativeStruct(ptr) {")
        indent {
            out("")
            out("companion object : Type<$className>(${s.size}, ::$className)")
            out("")
            s.fieldList.forEach { field ->
                try {
                    if (field.offset < 0) throw NotImplementedError();
                    assert(field.offset % 8 == 0L)
                    val offset = field.offset / 8
                    val fieldRefType = getKotlinNativeRefType(parseType(field.type))
                    out("val ${field.name} by ${fieldRefType.typeExpr} at $offset")
                } catch (e: Throwable) {
                    println("Warning: cannot generate definition for field $className.${field.name}")
                }
            }
        }
        out("}")
    }

    private fun generateKotlinForwardStruct(s: NativeIndex.CForwardStruct) {
        val className = s.mangledName
        out("class $className(ptr: NativePtr) : NativeRef(ptr) {")
        out("    companion object : Type<$className>(::$className)")
        out("}")
    }

    private fun generateKotlinEnum(e: NativeIndex.CEnum) {
        out("enum class ${e.name}(val value: Long) {")
        indent {
            e.valueList.forEach {
                out("${it.name}(${it.value}),")
            }
            out(";")
            out("")
            out("companion object {")
            out("    fun byValue(value: Long) = ${e.name}.values().find { it.value == value }!!")
            out("}")
        }
        out("}")
    }

    private fun retValBinding(func: NativeIndex.Function) = getInValueBinding(parseType(func.returnType))

    private fun paramBindings(func: NativeIndex.Function): Array<OutValueBinding> {
        val paramBindings = func.parameterList.map { param ->
            getOutValueBinding(parseType(param.type))
        }.toTypedArray()
        return paramBindings
    }

    private fun paramNames(func: NativeIndex.Function): Array<String> {
        val paramNames = func.parameterList.mapIndexed { i: Int, parameter: NativeIndex.Function.Parameter ->
            if (parameter.name != "") parameter.name else "arg$i"
        }.toTypedArray()
        return paramNames
    }

    private fun generateKotlinBindingMethod(func: NativeIndex.Function) {
        val paramNames = paramNames(func)
        val paramBindings = paramBindings(func)
        val retValBinding = retValBinding(func)

        val args = paramNames.mapIndexed { i: Int, name: String ->
            "$name: " + paramBindings[i].kotlinType
        }.joinToString(", ")

        out("fun ${func.name}($args): ${retValBinding.kotlinType} {")
        indent {
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
            paramNames.forEachIndexed { i, name ->
                val binding = paramBindings[i]
                if (binding.convFree != null) {
                    assert(binding.kotlinConv != null)
                    out(binding.convFree.invoke(externalParamNames[i]))
                }
            }
            out("return " + retValBinding.conv("res"))
        }
        out("}")
    }

    private fun generateKotlinExternalMethod(func: NativeIndex.Function) {
        val paramNames = paramNames(func)
        val paramBindings = paramBindings(func)
        val retValBinding = retValBinding(func)

        val args = paramNames.mapIndexed { i: Int, name: String ->
            "$name: " + paramBindings[i].kotlinJniBridgeType
        }.joinToString(", ")

        out("external fun ${func.name}($args): ${retValBinding.kotlinJniBridgeType}")
    }

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

        val generatedStructs = mutableSetOf<String>()

        translationUnit.structList.uniqueBy { it.name }.forEach { s ->
            try {
                transaction {
                    generateKotlinStruct(s)
                    out("")
                    generatedStructs.add(s.name)
                }
            } catch (e: Throwable) {
                println("Warning: cannot generate definition for struct ${s.name}")
            }
        }

        translationUnit.forwardStructList
                .uniqueBy { it.name }
                .filter { it.name !in generatedStructs }
                .forEach { s ->
                    generateKotlinForwardStruct(s)
                    out("")
                }

        translationUnit.enumList.uniqueBy { it.name }.forEach { e ->
            generateKotlinEnum(e)
            out("")
        }

        out("object externals {")
        indent {
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
        out("}")
    }

    fun getCJniBridgeType(kotlinJniBridgeType: String) = when (kotlinJniBridgeType) {
        "Unit" -> "void"
        "Byte" -> "jbyte"
        "Short" -> "jshort"
        "Int" -> "jint"
        "Long" -> "jlong"
        else -> throw NotImplementedError(kotlinJniBridgeType)
    }


    fun generateCFile(headerFiles: List<String>, translationUnit: NativeIndex.TranslationUnit) {
        out("#include <stdint.h>")
        out("#include <jni.h>")
        headerFiles.forEach {
            out("#include <$it>")
        }
        out("")

        functionsToBind.forEach { func ->
            try {
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

                val params = func.parameterList.mapIndexed { i, parameter ->
                    val cType = parseType(parameter.type).getStringRepresentation()
                    "($cType)${paramNames[i]}"
                }.joinToString(", ")

                val callExpr = "${func.name}($params)"
                val funcFullName = if (pkgName.isEmpty()) {
                    "externals.${func.name}"
                } else {
                    "$pkgName.externals.${func.name}"
                }
                val jniFuncName = "Java_" + funcFullName.replace("_", "_1").replace('.', '_').replace("$", "_00024")

                out("JNIEXPORT $cReturnType JNICALL $jniFuncName (JNIEnv *env, jobject obj$args) {")
                if (cReturnType == "void") {
                    out("    $callExpr;")
                } else {
                    out("    return ($cReturnType) ($callExpr);")
                }
                out("}")
            } catch (e: Throwable) {
                System.err.println("Warning: cannot generate C JNI function definition ${func.name}")
            }
        }
    }
}