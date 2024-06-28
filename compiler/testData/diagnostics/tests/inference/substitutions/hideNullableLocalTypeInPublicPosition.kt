// ISSUE: KT-30054
// FIR_IDENTICAL
// LANGUAGE: +KeepNullabilityWhenApproximatingLocalType
interface I {
    fun foo(): String
}

fun bar(condition: Boolean) /*: I? */ =
    if (condition)
        object : I {
            override fun foo() = "should check for null first"
            fun baz() = "invisible"
        }
    else null

fun main() {
    bar(false).<!UNRESOLVED_REFERENCE!>baz<!>()
    bar(false)<!UNSAFE_CALL!>.<!>foo()
    bar(false)?.foo()
}
