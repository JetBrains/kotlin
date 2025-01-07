// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// FILE: a.kt
package a

class Foo

fun acceptFoo(f: Foo) {}

// FILE: b.kt
package b

class Foo

fun test() {
    a.acceptFoo(<!TYPE_MISMATCH!>Foo()<!>)
}

fun <Foo> test2(f: Foo) {
    a.acceptFoo(<!TYPE_MISMATCH!>f<!>)
}