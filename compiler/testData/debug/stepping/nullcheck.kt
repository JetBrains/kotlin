


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

// EXPECTATIONS WASM
// test.kt:6 $box (10)
// test.kt:7 $box (9, 4)
// test.kt:13 $test (37)
// test.kt:14 $test (11, 4)
// test.kt:16 $test (12, 21, 31)
// test.kt:8 $box (9, 4)
// test.kt:9 $box (23, 4)
// test.kt:20 $testExpressionBody (4)
// test.kt:22 $testExpressionBody (12, 21, 31)
// test.kt:10 $box (23, 4)
// test.kt:11 $box (1)
