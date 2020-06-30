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

// LINENUMBERS
// test.kt:18 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:4 foo
// LINENUMBERS JVM
// test.kt:5 foo
// LINENUMBERS
// test.kt:6 foo
// test.kt:4 foo
// LINENUMBERS JVM
// test.kt:5 foo
// test.kt:6 foo
// LINENUMBERS
// test.kt:7 foo
// test.kt:10 foo
// LINENUMBERS JVM
// test.kt:11 foo
// test.kt:12 foo
// LINENUMBERS
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:6 foo
// test.kt:10 foo
// LINENUMBERS JVM
// test.kt:11 foo
// LINENUMBERS
// test.kt:12 foo
// test.kt:4 foo
// LINENUMBERS JVM
// test.kt:5 foo
// test.kt:6 foo
// LINENUMBERS
// test.kt:7 foo
// test.kt:10 foo
// LINENUMBERS JVM
// test.kt:11 foo
// test.kt:12 foo
// LINENUMBERS
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:12 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:4 foo
// LINENUMBERS JVM
// test.kt:5 foo
// LINENUMBERS
// test.kt:6 foo
// test.kt:4 foo
// LINENUMBERS JVM
// test.kt:5 foo
// test.kt:6 foo
// LINENUMBERS
// test.kt:7 foo
// test.kt:10 foo
// LINENUMBERS JVM
// test.kt:11 foo
// test.kt:12 foo
// LINENUMBERS
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:6 foo
// test.kt:10 foo
// LINENUMBERS JVM
// test.kt:11 foo
// LINENUMBERS
// test.kt:12 foo
// test.kt:4 foo
// LINENUMBERS JVM
// test.kt:5 foo
// test.kt:6 foo
// LINENUMBERS
// test.kt:7 foo
// test.kt:10 foo
// LINENUMBERS JVM
// test.kt:11 foo
// test.kt:12 foo
// LINENUMBERS
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:12 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:11 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:19 box