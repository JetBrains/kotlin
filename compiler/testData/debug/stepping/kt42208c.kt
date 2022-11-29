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
// test.kt:4 box
// test1.kt:10 box
// test1.kt:11 box
// test.kt:4 box
// test3.kt:14 baz
// EXPECTATIONS JVM_IR
// test1.kt:11 invoke
// test3.kt:14 baz
// EXPECTATIONS JVM JVM_IR
// test3.kt:15 baz
// test.kt:5 box
// test1.kt:10 box
// test1.kt:11 box
// test.kt:5 box
// test.kt:6 box
// EXPECTATIONS JVM_IR
// test3.kt:14 baz
// test1.kt:11 invoke
// EXPECTATIONS JVM JVM_IR
// test3.kt:14 baz
// test3.kt:15 baz
// test.kt:7 box

// EXPECTATIONS JS_IR
// test1.kt:11 box
// test.kt:4 box
// test3.kt:14 baz
// test1.kt:8 box$lambda
// test3.kt:15 baz
// test1.kt:11 box
// test.kt:5 box
// test.kt:6 box
// test3.kt:14 baz
// test1.kt:8 box$lambda
// test3.kt:15 baz
// test.kt:7 box
