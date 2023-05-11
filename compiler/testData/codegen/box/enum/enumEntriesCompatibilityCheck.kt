// !LANGUAGE: +EnumEntries
// IGNORE_BACKEND: JS, JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE
// IGNORE_LIGHT_ANALYSIS
// FULL_JDK
// WITH_STDLIB

package pckg

import kotlin.test.assertEquals

enum class EBasic {
    A0;
}

enum class E0 {;
    companion object {
        val entries = "OK"
    }
}

object Shadowing {
    val entries = "OK"

    enum class E0 {
        E;

        fun test() = entries
    }
}

enum class E01 {;
    object entries {
        override fun toString(): String {
            return "OK"
        }
    }
}

enum class E02(val entries: String) {
    E("OK");
    fun test() = entries // check whether this entries is referenced to ctor parameter
}

var e03Res: String? = null

enum class E03 {
    E("OK");

    constructor(entries: String) {
        e03Res = entries
    }
}

enum class E04 {
    E;
    val entries = "OK"
    fun test() = entries
}

// This case doesn't work in KJS/IR: KT-58396
interface I05 {
    val entries: String
        get() = "OK"
}

enum class E05 : I05 {
    E;

    fun test() = entries
}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    assertEquals(EBasic.entries.first().toString(), "A0") // make sure the feature is working

    assertEquals(E0.entries, "OK")
    assertEquals(Shadowing.E0.E.test(), "OK")
    assertEquals(E01.entries.toString(), "OK")
    assertEquals(E02.E.test(), "OK")

    E03.E // to initialize e03Res
    assertEquals(e03Res, "OK")

    assertEquals(E04.E.entries, "OK")
    assertEquals(E04.E.test(), "OK")
    assertEquals(E05.E.test(), "OK")
    return "OK"
}
