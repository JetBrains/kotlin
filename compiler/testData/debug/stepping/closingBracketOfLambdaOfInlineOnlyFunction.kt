// See KT-64725
// IGNORE_INLINER_K2: IR
// WITH_STDLIB
// FILE: test.kt

fun foo(block: () -> String): String = block()
inline fun bar(crossinline f: () -> String) = foo { f() }

fun flaf() {
    bar {
        run {
            "B"
        } // Should be present
    }
}

fun box() {
    flaf()
}

// EXPECTATIONS JVM_IR
// test.kt:18 box
// test.kt:10 flaf
// test.kt:7 flaf
// test.kt:6 foo
// test.kt:7 invoke
// test.kt:11 invoke
// test.kt:12 invoke
// test.kt:11 invoke
// test.kt:13 invoke
// test.kt:7 invoke
// test.kt:6 foo
// test.kt:7 flaf
// test.kt:15 flaf
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:18 box
// test.kt:7 flaf
// test.kt:6 foo
// test.kt:7 flaf$lambda
// test.kt:15 flaf
// test.kt:19 box

// EXPECTATIONS WASM
// test.kt:18 $box
// test.kt:10 $flaf
// test.kt:7 $flaf (46, 57)
// test.kt:6 $foo (39, 39, 39, 46)
// test.kt:7 $flaf$lambda.invoke (52, 55)
// test.kt:11 $flaf$lambda.invoke
// test.kt:12 $flaf$lambda.invoke (12, 12, 15)
// test.kt:13 $flaf$lambda.invoke
// test.kt:15 $flaf
// test.kt:19 $box
