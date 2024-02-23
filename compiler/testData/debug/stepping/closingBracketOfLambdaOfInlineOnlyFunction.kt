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
// test.kt:6 $foo (39, 46)
// test.kt:7 $flaf$lambda.invoke (52, 55)
// kotlin-package.kt:6 $flaf$lambda.invoke
// kotlin-package.kt:11 $flaf$lambda.invoke (917, 1002, 1002, 995)
// kotlin-package.kt:8 $flaf$lambda.invoke (3, 3, 3, 3, 6, 16)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set
// String.kt:149 $kotlin.stringLiteral (11, 4)
// test.kt:15 $flaf
// test.kt:19 $box
