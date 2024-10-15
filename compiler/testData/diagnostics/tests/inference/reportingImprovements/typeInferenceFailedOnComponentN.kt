// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

class X

operator fun <T> X.component1(): T = TODO()

fun test() {
    val (y) = <!COMPONENT_FUNCTION_MISSING!>X()<!>
}
