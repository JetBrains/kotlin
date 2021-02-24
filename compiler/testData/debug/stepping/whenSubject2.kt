// FILE: test.kt

fun foo(x: Int) {
    when
        (x)
    {
        21 -> foo(42)
        42 -> foo(63)
        else -> 1
    }

    val t = when
        (x)
    {
        21 -> foo(42)
        42 -> foo(63)
        else -> 2
    }
}

fun box() {
    foo(21)
}

// JVM_IR uses the line number of the when for the table switch and therefore,
// it stops on the subject line first, then on the when line (line 4 and 12), and
// then goes to the right branch.

// LINENUMBERS
// test.kt:22 box
// test.kt:5 foo
// LINENUMBERS JVM_IR
// test.kt:4 foo
// LINENUMBERS
// test.kt:7 foo
// test.kt:5 foo
// LINENUMBERS JVM_IR
// test.kt:4 foo
// LINENUMBERS
// test.kt:8 foo
// test.kt:5 foo
// LINENUMBERS JVM_IR
// test.kt:4 foo
// LINENUMBERS
// test.kt:9 foo
// test.kt:13 foo
// LINENUMBERS JVM_IR
// test.kt:12 foo
// LINENUMBERS
// test.kt:17 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:8 foo
// test.kt:13 foo
// LINENUMBERS JVM_IR
// test.kt:12 foo
// LINENUMBERS
// test.kt:16 foo
// test.kt:5 foo
// LINENUMBERS JVM_IR
// test.kt:4 foo
// LINENUMBERS
// test.kt:9 foo
// test.kt:13 foo
// LINENUMBERS JVM_IR
// test.kt:12 foo
// LINENUMBERS
// test.kt:17 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:16 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:7 foo
// test.kt:13 foo
// LINENUMBERS JVM_IR
// test.kt:12 foo
// LINENUMBERS
// test.kt:15 foo
// test.kt:5 foo
// LINENUMBERS JVM_IR
// test.kt:4 foo
// LINENUMBERS
// test.kt:8 foo
// test.kt:5 foo
// LINENUMBERS JVM_IR
// test.kt:4 foo
// LINENUMBERS
// test.kt:9 foo
// test.kt:13 foo
// LINENUMBERS JVM_IR
// test.kt:12 foo
// LINENUMBERS
// test.kt:17 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:8 foo
// test.kt:13 foo
// LINENUMBERS JVM_IR
// test.kt:12 foo
// LINENUMBERS
// test.kt:16 foo
// test.kt:5 foo
// LINENUMBERS JVM_IR
// test.kt:4 foo
// LINENUMBERS
// test.kt:9 foo
// test.kt:13 foo
// LINENUMBERS JVM_IR
// test.kt:12 foo
// LINENUMBERS
// test.kt:17 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:16 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:15 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:23 box
