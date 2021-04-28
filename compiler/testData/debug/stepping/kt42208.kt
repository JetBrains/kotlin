// FILE: test.kt

fun box() {
    foo()()
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}
// LINENUMBERS
// test.kt:4 box
// test1.kt:3 box
// test1.kt:4 box
// LINENUMBERS JVM_IR
// test.kt:4 box
// test1.kt:4 invoke
// test.kt:4 box
// LINENUMBERS
// test.kt:5 box