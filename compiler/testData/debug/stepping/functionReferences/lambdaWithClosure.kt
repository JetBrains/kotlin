// WITH_STDLIB

// FILE: test.kt
fun foo(x: Int): () -> Unit = {
    println(x)
}

fun box() {
    val lambda = foo(199)
    lambda()
}

// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:4 foo
// test.kt:6 foo
// test.kt:9 box
// test.kt:10 box
// test.kt:5 foo$lambda$0
// test.kt:6 foo$lambda$0
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:6 foo
// test.kt:10 box
// test.kt:5 foo$lambda$lambda
// test.kt:6 foo$lambda$lambda
// test.kt:11 box

// EXPECTATIONS WASM
// test.kt:9 $box (21, 17)
// test.kt:4 $foo (30)
// test.kt:6 $foo (1)
// test.kt:10 $box (4)
// test.kt:4 $foo$lambda.invoke (30)
// test.kt:5 $foo$lambda.invoke (12, 4)
// test.kt:4 $foo$lambda.invoke (30)
// test.kt:10 $box (4)
// test.kt:11 $box (1)

// EXPECTATIONS NATIVE
// test.kt:9 box
// test.kt:4 foo
// test.kt:6 foo
// test.kt:9 box
// test.kt:10 box
// test.kt:4 invoke
// test.kt:5 invoke
// test.kt:5 invoke
// test.kt:6 invoke
// test.kt:11 box
