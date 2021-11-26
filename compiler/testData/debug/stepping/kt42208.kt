// FILE: test.kt

fun box() {
    foo()()
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}
// EXPECTATIONS
// test.kt:4 box
// test1.kt:8 box
// test1.kt:9 box
// EXPECTATIONS JVM_IR
// test.kt:4 box
// test1.kt:9 invoke
// test.kt:4 box
// EXPECTATIONS
// test.kt:5 box
