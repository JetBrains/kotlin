// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <A1> fun2(f: kotlin.reflect.KFunction1<A1, Unit>, a: A1) {
    f.invoke(a)
}

fun containsRegex(vararg otherPatterns: String) {}

fun main() {
    fun2(::containsRegex, arrayOf("foo"))
}
