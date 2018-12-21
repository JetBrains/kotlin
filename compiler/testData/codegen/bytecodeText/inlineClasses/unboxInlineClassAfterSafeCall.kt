// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

// FILE: utils.kt

inline class Foo(val x: Int) {
    fun member() {}
}

// FILE: test.kt

fun Foo.extension() {}
fun <T> T.genericExtension() {}

fun test(f: Foo?) {
    f?.member() // unbox
    f?.extension() // unbox
    f?.genericExtension()
}

// @TestKt.class:
// 0 INVOKESTATIC Foo\$Erased.box
// 2 INVOKEVIRTUAL Foo.unbox

// 0 valueOf
// 0 intValue