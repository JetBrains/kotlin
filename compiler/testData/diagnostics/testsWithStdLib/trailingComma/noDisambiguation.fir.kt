// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER
// !LANGUAGE: +TrailingCommas

fun foo(vararg x: Int) = false
fun foo() = true

fun main() {
    val x = foo()
    val y = foo(<!SYNTAX!>,<!><!SYNTAX!><!>)
}
