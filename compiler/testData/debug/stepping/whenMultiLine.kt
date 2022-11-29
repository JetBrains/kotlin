// FILE: test.kt

fun foo(x: Int): Int {
    when {
        x == 21 ->
            1
        x == 42 ->
            2
        else ->
            3
    }

    val t = when {
        x == 21 ->
            1
        x == 42 ->
            2
        else ->
            3
    }

    return t
}

fun box() {
    foo(21)
    foo(42)
    foo(63)
}

// JVM_IR backend optimized the when to a switch in the java bytecode.
// Therefore, the stepping for JVM_IR does not step through the evaluation
// of each of the conditions, but goes directly to the right body.
// JVM_IR stepping behavior here is the same as for `whenMultiLineSubject.kt`.

// EXPECTATIONS JVM JVM_IR
// test.kt:26 box
// test.kt:4 foo
// EXPECTATIONS JVM
// test.kt:5 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:6 foo
// test.kt:13 foo
// EXPECTATIONS JVM
// test.kt:14 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:15 foo
// test.kt:13 foo
// test.kt:22 foo
// test.kt:26 box
// test.kt:27 box
// test.kt:4 foo
// EXPECTATIONS JVM
// test.kt:5 foo
// test.kt:7 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:8 foo
// test.kt:13 foo
// EXPECTATIONS JVM
// test.kt:14 foo
// test.kt:16 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:17 foo
// test.kt:13 foo
// test.kt:22 foo
// test.kt:27 box
// test.kt:28 box
// test.kt:4 foo
// EXPECTATIONS JVM
// test.kt:5 foo
// test.kt:7 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:10 foo
// test.kt:13 foo
// EXPECTATIONS JVM
// test.kt:14 foo
// test.kt:16 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:19 foo
// test.kt:13 foo
// test.kt:22 foo
// test.kt:28 box
// test.kt:29 box

// EXPECTATIONS JS_IR
// test.kt:26 box
// test.kt:4 foo
// test.kt:13 foo
// test.kt:15 foo
// test.kt:22 foo
// test.kt:27 box
// test.kt:4 foo
// test.kt:13 foo
// test.kt:17 foo
// test.kt:22 foo
// test.kt:28 box
// test.kt:4 foo
// test.kt:13 foo
// test.kt:19 foo
// test.kt:22 foo
// test.kt:29 box
