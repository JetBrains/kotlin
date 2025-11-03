// LANGUAGE: +ConditionImpliesReturnsContracts
// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: declaration.kt
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

fun decode(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    if (encoded == null) return null
    return encoded + "a"
}

fun decodeFake(encoded: String?): String? {
    contract {
        (encoded == null) implies (returnsNotNull())
    }
    return if (encoded == null) "hello" else null
}

fun decodeIfString(encoded: Any): String? {
    contract {
        (encoded is String) implies (returnsNotNull())
    }
    return when (encoded) {
        is String -> encoded + "a"
        else -> null
    }
}

fun decodeNotNumber(encoded: Any): String? {
    contract {
        (encoded !is Number) implies (returnsNotNull())
    }
    return when (encoded) {
        is Number -> null
        else -> encoded.toString()
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun testVar() {
    val x: String = ""
    decode(x).length
}

fun testLiteral() {
    decode("").length
}
fun testFake() {
    val x = null
    decodeFake(x).length
}
fun tesStringOrChar() {
    decodeIfString("abc").length
}

fun testNotNumber(x: Any) {
    if (x !is Number)
        decodeNotNumber(x).length
    decodeNotNumber("abc").length
}
