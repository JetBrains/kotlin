


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
// test.kt:6 box
// test.kt:15 test
// test.kt:13 test
// test.kt:6 box

// test.kt:7 box
// test.kt:15 test
// test.kt:13 test
// test.kt:7 box

// test.kt:8 box
// test.kt:21 testExpressionBody
// test.kt:8 box

// test.kt:9 box
// test.kt:21 testExpressionBody
// test.kt:9 box

// test.kt:10 box

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:13 test
// test.kt:15 test
// test.kt:13 test
// test.kt:6 box

// test.kt:7 box
// test.kt:13 test
// test.kt:15 test
// test.kt:13 test
// test.kt:7 box

// test.kt:8 box
// test.kt:19 testExpressionBody
// test.kt:21 testExpressionBody
// test.kt:8 box

// test.kt:9 box
// test.kt:19 testExpressionBody
// test.kt:21 testExpressionBody
// test.kt:9 box

// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:13 test
// test.kt:7 box
// test.kt:13 test
// test.kt:8 box
// test.kt:21 testExpressionBody
// test.kt:9 box
// test.kt:21 testExpressionBody
// test.kt:10 box