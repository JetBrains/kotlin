package org.jetbrains.kotlin.backend.konan.cgen

interface CType {
    fun render(name: String): String
}

class CVariable(val type: CType, val name: String) {
    override fun toString() = type.render(name)
}

object CTypes {
    fun simple(type: String): CType = SimpleCType(type)
    fun pointer(pointee: CType): CType = PointerCType(pointee)
    fun function(returnType: CType, parameterTypes: List<CType>, variadic: Boolean): CType =
            FunctionCType(returnType, parameterTypes, variadic)

    fun blockPointer(pointee: CType): CType = object : CType {
        override fun render(name: String): String = pointee.render("^$name")
    }

    val void = simple("void")
    val voidPtr = pointer(void)
    val signedChar = simple("signed char")
    val unsignedChar = simple("unsigned char")
    val short = simple("short")
    val unsignedShort = simple("unsigned short")
    val int = simple("int")
    val unsignedInt = simple("unsigned int")
    val longLong = simple("long long")
    val unsignedLongLong = simple("unsigned long long")
    val float = simple("float")
    val double = simple("double")
    val C99Bool = simple("_Bool")
    val char = simple("char")

    val vector128 = simple("float __attribute__ ((__vector_size__ (16)))")

    val id = simple("id")
}

private class SimpleCType(private val type: String) : CType {
    override fun render(name: String): String = if (name.isEmpty()) type else "$type $name"
}

private class PointerCType(private val pointee: CType) : CType {
    override fun render(name: String): String = pointee.render("*$name")
}

private class FunctionCType(
        private val returnType: CType,
        private val parameterTypes: List<CType>,
        private val variadic: Boolean
) : CType {
    override fun render(name: String): String = returnType.render(buildString {
        append("(")
        append(name)
        append(")(")
        parameterTypes.joinTo(this) { it.render("") }
        if (parameterTypes.isEmpty()) {
            if (!variadic) append("void")
        } else {
            if (variadic) append(", ...")
        }
        append(')')
    })
}

/**
 * The implementation is conservative (escapes more than necessary) but acts within the C standard.
 */
internal fun quoteAsCStringLiteral(str: String): String = buildString {
    append('"')
    // Encoding the string to UTF-8 is arguable (e.g. what if the target platform uses another encoding?),
    // but we anyway encode the generated C stubs to UTF-8 when writing them to file in CStubsManager.
    // So here we just do the same for fragments of them but earlier.
    for (byte in str.toByteArray(Charsets.UTF_8)) {
        when (byte) {
            in asciiCharsAllowedInCStringLiterals -> {
                // It is an allowed ASCII char and therefore can be rendered as is.
                append(byte.toInt().toChar())
            }
            else -> {
                // Some potentially arbitrary 8-bit value.
                // Encode it as a three-digit octal escape sequence, e.g. `$` -> `\044`.
                val threeOctalDigits = byte.toUByte().toString(radix = 8).padStart(length = 3, padChar = '0')
                append("\\$threeOctalDigits")
                // We could potentially use hex escape sequences (e.g. `\x24`). But they are tricky:
                // we can't use a character that happens to be a hex digit right after the sequence,
                // because `\x24f` is parsed as a single escape sequence and fails the compilation being out of range.
            }
        }
    }
    append('"')
}

private val asciiCharsAllowedInCStringLiterals: Set<Byte> = buildSet {
    addAll('A'..'Z')
    addAll('a'..'z')
    addAll('0'..'9')
    addAll("!#%&'()*+,-./:;<=>?[]^_{|}~ ".toList())
}.mapTo(HashSet()) {
    val code = it.code
    check(code in 0..<128) { "Allowed char is not ASCII: $it" }
    code.toByte()
}
