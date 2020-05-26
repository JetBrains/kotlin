// FILE: test.kt

fun foo() {
    try {
        mightThrow()
    } catch (e: Throwable) {
        return
    }
    
    val t = try {
        mightThrow2()
    } catch (e: Throwable) {
        return
    }
}

var throw1 = false
var throw2 = false

fun mightThrow() {
    if (throw1) throw Exception()
}

fun mightThrow2() {
    if (throw2) throw Exception()
}

fun box() {
    foo()
    throw2 = true
    foo()
    throw1 = true
    foo()
}

// IGNORE_BACKEND: JVM_IR
// The JVM_IR backend will stop on line 13 even when mightThrow2
// does not throw!

// LINENUMBERS
// test.kt:29 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:21 mightThrow
// test.kt:22 mightThrow
// test.kt:5 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:25 mightThrow2
// test.kt:26 mightThrow2
// test.kt:11 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:30 box
// test.kt:31 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:21 mightThrow
// test.kt:22 mightThrow
// test.kt:5 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:25 mightThrow2
// test.kt:12 foo
// test.kt:13 foo
// test.kt:32 box
// test.kt:33 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:21 mightThrow
// test.kt:6 foo
// test.kt:7 foo
// test.kt:34 box
