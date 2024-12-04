
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

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:5 box
// test.kt:15 foo
// test.kt:6 invoke
// test.kt:7 invoke
// test.kt:15 foo
// test.kt:16 foo
// test.kt:9 box
// test.kt:15 foo
// test.kt:10 invoke
// test.kt:11 invoke
// test.kt:15 foo
// test.kt:16 foo
// test.kt:12 box

// EXPECTATIONS FIR JVM_IR
// test.kt:5 box
// test.kt:15 foo
// test.kt:6 box$lambda$0
// test.kt:7 box$lambda$0
// test.kt:15 foo
// test.kt:16 foo
// test.kt:5 box
// test.kt:9 box
// test.kt:15 foo
// test.kt:10 box$lambda$1
// test.kt:11 box$lambda$1
// test.kt:15 foo
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
// test.kt:5 $box
// test.kt:15 $foo (4, 4, 4, 4, 4, 4)
// test.kt:6 $box$lambda.invoke (20, 12, 21)
// test.kt:16 $foo (1, 1)
// test.kt:9 $box
// test.kt:10 $box$lambda.invoke (16, 8, 17)
// test.kt:12 $box
