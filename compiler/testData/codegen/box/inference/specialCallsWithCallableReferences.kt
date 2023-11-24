// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// FIR status: Disabling of StrictOnlyInputTypesChecks is not supported by FIR
// WITH_STDLIB
// SKIP_TXT
// !LANGUAGE: -StrictOnlyInputTypesChecks

import kotlin.experimental.ExperimentalTypeInference

fun <K> K.bar3(): K = null as K
fun <K> K.foo3(): K = null as K

fun bar2(): Int = 1
fun foo2(): Float = 1f

fun <K> bar4(): K = null as K
fun <K> foo4(): K = null as K

class Foo6

class Foo7<T>
fun foo7() = null as Foo7<Int>

fun poll1(flag: Boolean): Any? {
    val inv = if (flag) { ::bar2 } else { ::foo2 }
    return inv()
}

fun poll11(flag: Boolean): Any? {
    val inv = if (flag) { ::bar2 } else { ::foo2 }
    return inv()
}

fun poll16(flag: Boolean): Any? {
    val inv = if (flag) { ::Foo6 } else { ::Foo6 }
    return inv()
}

fun poll21(flag: Boolean): Any? {
    val inv = when (flag) { true -> ::bar2 else -> ::foo2 }
    return inv()
}

fun poll25(flag: Boolean): Any? {
    val inv = when (flag) { true -> ::Foo6 else -> ::Foo6 }
    return inv
}

fun poll31(flag: Boolean): Any? {
    val inv = when (flag) { true -> ::bar2 false -> ::foo2 }
    return inv()
}

fun poll35(flag: Boolean): Any? {
    val inv = when (flag) { true -> ::Foo6 false -> ::Foo6 }
    return inv
}

fun poll41(): Any? {
    val inv = try { ::bar2 } finally { ::foo2 }
    return inv()
}

fun poll45(): Any? {
    val inv = try { ::Foo6 } finally { ::Foo6 }
    return inv()
}

fun poll51(): Any? {
    val inv = try { ::bar2 } catch (e: Exception) { ::foo2 } finally { ::foo2 }
    return inv()
}

fun poll55(): Any? {
    val inv = try { ::Foo6 } catch (e: Exception) { ::Foo6 } finally { ::Foo6 }
    return inv()
}

fun poll61(): Any? {
    val inv = ::bar2
    return inv
}

fun poll65(): Any? {
    val inv = ::Foo6
    return inv
}

fun poll71(): Any? {
    val inv = ::bar2!!
    return inv()
}

fun poll75(): Any? {
    val inv = ::Foo6!!
    return inv
}

fun poll81(): Any? {
    val inv = ::bar2 in setOf(::foo2)
    return inv
}

fun poll85(): Any? {
    val inv = ::Foo6 in setOf(::Foo6)
    return inv
}

fun box(): String {
    poll1(true)
    poll11(true)
    poll16(true)
    poll21(true)
    poll25(true)
    poll31(true)
    poll35(true)
    poll41()
    poll45()
    poll51()
    poll55()
    poll61()
    poll65()
    poll71()
    poll75()
    poll81()
    poll85()
    return "OK"
}

