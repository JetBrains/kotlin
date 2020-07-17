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

// IGNORE_BACKEND: JVM_IR

// The JVM_IR backend has line number 8 when leaving the first
// when. Also, the stepping is different, probably because the when
// is compiled to a switch which it isn't with JVM? The stepping
// behavior is likely OK, but should be double checked.

// LINENUMBERS
// test.kt:18 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:4 foo
// test.kt:5 foo
// test.kt:6 foo
// test.kt:4 foo
// test.kt:5 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:6 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:4 foo
// test.kt:5 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:12 foo
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
// test.kt:5 foo
// test.kt:6 foo
// test.kt:4 foo
// test.kt:5 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:6 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:4 foo
// test.kt:5 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:12 foo
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
