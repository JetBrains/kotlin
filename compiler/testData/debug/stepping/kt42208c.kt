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
// LINENUMBERS
// test.kt:4 box
// test1.kt:3 box
// test1.kt:4 box
// test.kt:4 box
// test3.kt:3 baz
// LINENUMBERS JVM_IR
// test1.kt:4 invoke
// test3.kt:3 baz
// LINENUMBERS
// test3.kt:4 baz
// test.kt:5 box
// test1.kt:3 box
// test1.kt:4 box
// test.kt:5 box
// test.kt:6 box
// LINENUMBERS JVM_IR
// test3.kt:3 baz
// test1.kt:4 invoke
// LINENUMBERS
// test3.kt:3 baz
// test3.kt:4 baz
// test.kt:7 box