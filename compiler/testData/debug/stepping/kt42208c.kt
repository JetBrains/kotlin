// IGNORE_BACKEND: WASM
// IGNORE_INLINER: IR
// FILE: test.kt

fun box() {
    baz(foo())
    val a = foo()
    baz(a)
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}
//FILE: test3.kt
fun baz(v:(() -> Unit)) {
    v()
}
// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// test1.kt:12 box
// test1.kt:13 box
// test.kt:6 box
// test3.kt:16 baz
// EXPECTATIONS JVM_IR
// test1.kt:13 invoke
// test3.kt:16 baz
// EXPECTATIONS JVM JVM_IR
// test3.kt:17 baz
// test.kt:7 box
// test1.kt:12 box
// test1.kt:13 box
// test.kt:7 box
// test.kt:8 box
// EXPECTATIONS JVM_IR
// test3.kt:16 baz
// test1.kt:13 invoke
// EXPECTATIONS JVM JVM_IR
// test3.kt:16 baz
// test3.kt:17 baz
// test.kt:9 box

// EXPECTATIONS JS_IR
// test1.kt:12 box
// test.kt:6 box
// test3.kt:16 baz
// test.kt:8 box$lambda
// test3.kt:17 baz
// test1.kt:12 box
// test.kt:8 box
// test3.kt:16 baz
// test.kt:8 box$lambda
// test3.kt:17 baz
// test.kt:9 box
