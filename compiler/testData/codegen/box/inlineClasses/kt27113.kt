// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

class CharacterLiteral(private val prefix: NamelessString, private val s: NamelessString) {
    override fun toString(): String = "$prefix'$s'"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class NamelessString(val b: CharArray) {
    override fun toString(): String = b.concatToString()
}

fun box(): String {
    val abcCharArray = "abc".toCharArray()
    if (abcCharArray.toString() == "abc") return "Prerequisite failed: value of abcCharArray.toString() is intended to be not equal to 'abc'"
    val ns1 = NamelessString(abcCharArray)
    val ns2 = NamelessString("def".toCharArray())
    val cl = CharacterLiteral(ns1, ns2)
    if (cl.toString() != "abc'def'") return cl.toString()
    return "OK"
}
