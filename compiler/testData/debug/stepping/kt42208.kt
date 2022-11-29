// FILE: test.kt

fun box() {
    foo()()
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}
// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test1.kt:8 box
// test1.kt:9 box
// EXPECTATIONS JVM_IR
// test.kt:4 box
// test1.kt:9 invoke
// test.kt:4 box
// EXPECTATIONS JVM JVM_IR
// test.kt:5 box

// EXPECTATIONS JS_IR
// test1.kt:9 box
// test.kt:4 box
// test1.kt:6 box$lambda
// test.kt:5 box
