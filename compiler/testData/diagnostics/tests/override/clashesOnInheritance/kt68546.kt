// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68546
// DIAGNOSTICS: -ERROR_SUPPRESSION

fun main() {
    with(object : Scope2<Int> {}) {
        col()
        col<Int>()
    }
}

interface Scope2<out T> : Scope
interface Scope {

    fun <T> col() {
        println("typed")
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("colUnTyped")
    fun col() {
        println("untyped")
    }
}
