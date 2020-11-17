// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM, JVM_IR
// IGNORE_LIGHT_ANALYSIS
// MODULE: lib1
// FILE: lib1.kt

// KT-34273

var foo = "Fail"

// MODULE: lib2(lib1)
// FILE: lib2.kt

private val bar = run {
    foo = "OK"
    42
}


// MODULE: main(lib1, lib2)
// FILE: main.kt

// TODO: the proper behaviour of this test is still open design issue
// K/N & K/JS used to go like this when JVM fails

fun box(): String = foo
