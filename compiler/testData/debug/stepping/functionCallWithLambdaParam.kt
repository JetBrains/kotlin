
// FILE: test.kt

fun box() {
    foo({
            val a = 1
        })

    foo() {
        val a = 1
    }
}

fun foo(f: () -> Unit) {
    f()
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:15 foo
// test.kt:6 box$lambda$0
// test.kt:7 box$lambda$0
// test.kt:15 foo
// test.kt:16 foo
// test.kt:9 box
// test.kt:15 foo
// test.kt:10 box$lambda$1
// test.kt:11 box$lambda$1
// test.kt:15 foo
// test.kt:16 foo
// test.kt:12 box

// EXPECTATIONS NATIVE
// test.kt:5 box
// test.kt:14 foo
// test.kt:15 foo
// test.kt:5 invoke
// test.kt:6 invoke
// test.kt:7 invoke
// test.kt:16 foo
// test.kt:9 box
// test.kt:14 foo
// test.kt:15 foo
// test.kt:9 invoke
// test.kt:10 invoke
// test.kt:11 invoke
// test.kt:16 foo
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:15 foo
// test.kt:6 box$lambda
// test.kt:7 box$lambda
// test.kt:16 foo
// test.kt:9 box
// test.kt:15 foo
// test.kt:10 box$lambda
// test.kt:11 box$lambda
// test.kt:16 foo
// test.kt:12 box

// EXPECTATIONS WASM
// test.kt:5 $box (8, 4)
// test.kt:15 $foo (4)
// test.kt:6 $box$lambda.invoke (20)
// test.kt:5 $box$lambda.invoke (8)
// test.kt:15 $foo (4)
// test.kt:16 $foo (1)
// test.kt:9 $box (10, 4)
// test.kt:15 $foo (4)
// test.kt:10 $box$lambda.invoke (16)
// test.kt:9 $box$lambda.invoke (10)
// test.kt:15 $foo (4)
// test.kt:16 $foo (1)
// test.kt:12 $box (1)
