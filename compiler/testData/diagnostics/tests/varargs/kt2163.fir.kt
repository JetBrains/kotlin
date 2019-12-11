// !DIAGNOSTICS: -UNUSED_PARAMETER

fun printAll(vararg a : Any) {}

fun main(args: Array<String>) {
    <!INAPPLICABLE_CANDIDATE!>printAll<!>(*args) // Shouldn't be an error
}