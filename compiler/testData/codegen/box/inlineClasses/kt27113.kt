// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS, JS_IR, NATIVE
// IGNORE_BACKEND: JS_IR_ES6
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

class CharacterLiteral(private val prefix: NamelessString, private val s: NamelessString) {
    override fun toString(): String = "$prefix'$s'"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class NamelessString(val b: ByteArray) {
    override fun toString(): String = String(b)
}

fun box(): String {
    val ns1 = NamelessString("abc".toByteArray())
    val ns2 = NamelessString("def".toByteArray())
    val cl = CharacterLiteral(ns1, ns2)
    if (cl.toString() != "abc'def'") return throw AssertionError(cl.toString())
    return "OK"
}