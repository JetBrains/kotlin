// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB

interface A {
    fun foo(): MutableList<String>
}

@ExperimentalStdlibApi
fun main() {
    buildList {
        add(3)
        val x: String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>get(0)<!>
    }
}
