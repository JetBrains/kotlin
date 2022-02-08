// FILE: test.kt

fun box() {
    val a = foo()
    a()
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}
// EXPECTATIONS
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
