// IGNORE_BACKEND: WASM
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

// EXPECTATIONS JVM JVM_IR
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