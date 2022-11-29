// FILE: test.kt

fun box() {
    val a = foo()
    a()
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}
// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test1.kt:9 box
// test1.kt:10 box
// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:5 box
// test1.kt:10 invoke
// test.kt:5 box
// EXPECTATIONS JVM_IR
// test.kt:6 box
// EXPECTATIONS JVM
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test1.kt:10 box
// test.kt:4 box
// test.kt:5 box
// test1.kt:7 box$lambda
// test.kt:6 box
