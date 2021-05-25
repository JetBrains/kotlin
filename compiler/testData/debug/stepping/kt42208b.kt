// FILE: test.kt

fun box() {
    val a = foo()
    a()
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
// test.kt:5 box
// test1.kt:4 invoke
// test.kt:5 box
// LINENUMBERS JVM_IR
// test.kt:6 box
// LINENUMBERS JVM
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box