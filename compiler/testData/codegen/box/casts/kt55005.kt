// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_TEXT
// WITH_STDLIB
// IGNORE_BACKEND_K1: JVM_IR
// ^ K1 does not support coercing assigment to Any?

import kotlin.test.*

inline fun <reified T> foo(x: Any) = arrayOf<T>(x as T)

fun box(): String {
    val a: Array<String> = arrayOf("")

    assertFailsWith<ArrayStoreException> {
        (a as Array<Any>)[0] = Any()
    }
    assertFailsWith<ArrayStoreException> {
        (a as Array<Any>)[0] = 1
    }
    assertFailsWith<ArrayStoreException> {
        (a as Array<CharSequence>)[0] = StringBuilder()
    }
    assertFailsWith<ArrayStoreException> {
        foo<String>(Any())
    }
    return "OK"
}

// 0 CHECKCAST
