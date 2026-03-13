// TARGET_BACKEND: JVM
// ISSUE: KT-83587
// JVM_TARGET: 11
// JDK_KIND: FULL_JDK_11
// DUMP_IR

fun box(): String {
    attempt("bool") {
        val value = getNullAsPlatform(true)
        value == true
    }

    attempt("short") {
        val value = getNullAsPlatform(0.toShort())
        value == 0.toShort()
    }

    attempt("char") {
        val value = getNullAsPlatform(0.toChar())
        value == 0.toChar()
    }

    attempt("long") {
        val value = getNullAsPlatform(0L)
        value == 0L
    }

    attempt("int") {
        val value = getNullAsPlatform(0)
        value == 0
    }

    attempt("byte") {
        val value = getNullAsPlatform(0.toByte())
        value == 0.toByte()
    }
    return "OK"
}

inline fun attempt(name: String, block: () -> Boolean) {
    println(name + ": " + block())
}

// inferring the return type to get the platform type
fun <E> getNullAsPlatform(e: E) /*: E? */ = java.util.List.of(e).toMutableList().also { it.set(0, null) }.first()
