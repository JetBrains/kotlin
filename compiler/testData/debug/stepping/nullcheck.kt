// IGNORE_BACKEND: WASM


// FILE: test.kt

fun box() {
    test("OK")
    test(null)
    testExpressionBody("OK")
    testExpressionBody(null)
}

fun test(nullable: String?): Boolean {
    return nullable != null &&
            // Some comment
            nullable.length == 2
}

fun testExpressionBody(nullable: String?) =
    nullable != null &&
            // Some comment
            nullable.length == 2

// EXPECTATIONS JVM
// test.kt:7 box
// test.kt:16 test
// test.kt:14 test
// test.kt:7 box

// test.kt:8 box
// test.kt:16 test
// test.kt:14 test
// test.kt:8 box

// test.kt:9 box
// test.kt:22 testExpressionBody
// test.kt:9 box

// test.kt:10 box
// test.kt:22 testExpressionBody
// test.kt:10 box

// test.kt:11 box

// EXPECTATIONS JVM_IR
// test.kt:7 box
// test.kt:14 test
// test.kt:16 test
// test.kt:14 test
// test.kt:7 box

// test.kt:8 box
// test.kt:14 test
// test.kt:16 test
// test.kt:14 test
// test.kt:8 box

// test.kt:9 box
// test.kt:20 testExpressionBody
// test.kt:22 testExpressionBody
// test.kt:9 box

// test.kt:10 box
// test.kt:20 testExpressionBody
// test.kt:22 testExpressionBody
// test.kt:10 box

// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:14 test
// test.kt:8 box
// test.kt:14 test
// test.kt:9 box
// test.kt:22 testExpressionBody
// test.kt:10 box
// test.kt:22 testExpressionBody
// test.kt:11 box