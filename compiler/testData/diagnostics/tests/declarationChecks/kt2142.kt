// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
//KT-2142 function local classes do not work

package a

fun foo() {
    class Foo() {}
    Foo() // Unresolved reference Foo
}