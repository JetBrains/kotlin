// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// WITH_RUNTIME

class CharacterLiteral(private val prefix: NamelessString, private val s: NamelessString) {
    override fun toString(): String = "$prefix'$s'"
}

inline class NamelessString(val b: ByteArray) {
    override fun toString(): String = String(b)
}

fun box(): String {
    val ns1 = NamelessString("abc".toByteArray())
    val ns2 = NamelessString("def".toByteArray())
    val cl = CharacterLiteral(ns1, ns2)
    if (cl.toString() != "abc'def'") return throw AssertionError(cl.toString())
    return "OK"
}