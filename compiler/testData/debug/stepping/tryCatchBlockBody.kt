
// FILE: test.kt

var global: String? = null

fun foo(): String {
    var local: String? = null
    try {
        throw Exception()
    } catch (e: Exception) {
        fun nested(): String? = e.message

        local = nested()
        global = local
        return "OK"
    }
    return "FAIL"
}

fun box(): String =
    foo()

// EXPECTATIONS JVM_IR
// test.kt:21 box
// test.kt:7 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:11 foo$nested
// test.kt:13 foo
// test.kt:14 foo
// test.kt:15 foo
// test.kt:21 box

// EXPECTATIONS NATIVE
// test.kt:1 box
// test.kt:21 box
// test.kt:6 foo
// test.kt:1 foo
// test.kt:7 foo
// test.kt:9 foo
// test.kt:9 foo
// test.kt:18 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:11 foo$nested
// test.kt:1 foo$nested
// test.kt:11 foo$nested
// test.kt:11 foo$nested
// test.kt:13 foo
// test.kt:14 foo
// test.kt:4 <set-global>
// test.kt:1 <set-global>
// test.kt:4 <set-global>
// test.kt:15 foo
// test.kt:18 foo
// test.kt:21 box

// EXPECTATIONS JS_IR
// test.kt:21 box
// test.kt:7 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:11 foo$nested
// test.kt:14 foo
// test.kt:15 foo

// EXPECTATIONS WASM
// test.kt:21 $box (4)
// test.kt:7 $foo (25, 4)
// test.kt:9 $foo (14, 8)
// test.kt:13 $foo (16)
// test.kt:11 $nested (32, 34, 41)
// test.kt:14 $foo (17, 8)
// test.kt:15 $foo (15, 8)
// test.kt:21 $box (9)
