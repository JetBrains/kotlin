// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
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

    val x = try {
        mightThrow3()
    } catch (e: Throwable) {
        return
    }
}

var throw1 = false
var throw2 = false
var throw3 = false

fun mightThrow() {
    if (throw1) throw Exception()
}

fun mightThrow2() {
    if (throw2) throw Exception()
}

fun mightThrow3(): Int {
    if (throw3) throw Exception()
    return 42
}

fun box() {
    foo()
    throw3 = true
    foo()
    throw2 = true
    foo()
    throw1 = true
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:43 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:30 mightThrow
// test.kt:31 mightThrow
// test.kt:7 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:34 mightThrow2
// test.kt:35 mightThrow2
// test.kt:13 foo
// test.kt:12 foo
// test.kt:18 foo
// test.kt:19 foo
// test.kt:38 mightThrow3
// test.kt:39 mightThrow3
// test.kt:19 foo
// test.kt:18 foo
// test.kt:23 foo
// test.kt:44 box
// test.kt:45 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:30 mightThrow
// test.kt:31 mightThrow
// test.kt:7 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:34 mightThrow2
// test.kt:35 mightThrow2
// test.kt:13 foo
// test.kt:12 foo
// test.kt:18 foo
// test.kt:19 foo
// test.kt:38 mightThrow3
// test.kt:20 foo
// test.kt:21 foo
// test.kt:46 box
// test.kt:47 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:30 mightThrow
// test.kt:31 mightThrow
// test.kt:7 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:34 mightThrow2
// test.kt:14 foo
// test.kt:15 foo
// test.kt:48 box
// test.kt:49 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:30 mightThrow
// test.kt:8 foo
// test.kt:9 foo
// test.kt:50 box

// EXPECTATIONS JS_IR
// test.kt:43 box
// test.kt:7 foo
// test.kt:30 mightThrow
// test.kt:31 mightThrow
// test.kt:13 foo
// test.kt:34 mightThrow2
// test.kt:35 mightThrow2
// test.kt:12 foo
// test.kt:19 foo
// test.kt:38 mightThrow3
// test.kt:39 mightThrow3
// test.kt:18 foo
// test.kt:23 foo
// test.kt:44 box
// test.kt:45 box
// test.kt:7 foo
// test.kt:30 mightThrow
// test.kt:31 mightThrow
// test.kt:13 foo
// test.kt:34 mightThrow2
// test.kt:35 mightThrow2
// test.kt:12 foo
// test.kt:19 foo
// test.kt:38 mightThrow3
// test.kt:38 mightThrow3
// test.kt:20 foo
// test.kt:21 foo
// test.kt:46 box
// test.kt:47 box
// test.kt:7 foo
// test.kt:30 mightThrow
// test.kt:31 mightThrow
// test.kt:13 foo
// test.kt:34 mightThrow2
// test.kt:34 mightThrow2
// test.kt:14 foo
// test.kt:15 foo
// test.kt:48 box
// test.kt:49 box
// test.kt:7 foo
// test.kt:30 mightThrow
// test.kt:30 mightThrow
// test.kt:8 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:50 box
