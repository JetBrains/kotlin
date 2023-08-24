// IGNORE_BACKEND: WASM
// FILE: test.kt

fun foo(x: Int) {
    when {
        x == 21 -> foo(42)
        x == 42 -> foo(63)
        else -> 1
    }
    
    val t = when {
        x == 21 -> foo(42)
        x == 42 -> foo(63)
        else -> 2
    }
}

fun box() {
    foo(21)
}

// JVM_IR backend optimized the when to a switch in the java bytecode.
// Therefore, the stepping for JVM_IR does not step through the evaluation
// of each of the conditions, but goes directly to the right body. The
// JVM_IR stepping behavior here is the same as for `whenSubject.kt`.

// EXPECTATIONS JVM JVM_IR
// test.kt:19 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:5 foo
// EXPECTATIONS JVM
// test.kt:6 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:7 foo
// test.kt:5 foo
// EXPECTATIONS JVM
// test.kt:6 foo
// test.kt:7 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:8 foo
// test.kt:11 foo
// EXPECTATIONS JVM
// test.kt:12 foo
// test.kt:13 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:7 foo
// test.kt:11 foo
// EXPECTATIONS JVM
// test.kt:12 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:13 foo
// test.kt:5 foo
// EXPECTATIONS JVM
// test.kt:6 foo
// test.kt:7 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:8 foo
// test.kt:11 foo
// EXPECTATIONS JVM
// test.kt:12 foo
// test.kt:13 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:13 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:6 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:5 foo
// EXPECTATIONS JVM
// test.kt:6 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:7 foo
// test.kt:5 foo
// EXPECTATIONS JVM
// test.kt:6 foo
// test.kt:7 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:8 foo
// test.kt:11 foo
// EXPECTATIONS JVM
// test.kt:12 foo
// test.kt:13 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:7 foo
// test.kt:11 foo
// EXPECTATIONS JVM
// test.kt:12 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:13 foo
// test.kt:5 foo
// EXPECTATIONS JVM
// test.kt:6 foo
// test.kt:7 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:8 foo
// test.kt:11 foo
// EXPECTATIONS JVM
// test.kt:12 foo
// test.kt:13 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:13 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:12 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:20 box

// EXPECTATIONS JS_IR
// test.kt:19 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:20 box
