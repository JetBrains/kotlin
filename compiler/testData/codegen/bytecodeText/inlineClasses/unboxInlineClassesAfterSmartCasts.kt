// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

// FILE: utils.kt

inline class Foo(val x: Int) {
    fun member() {}
}

// FILE: test.kt

fun Foo?.extensionOnNullable() {}

fun test(f: Foo?) {
    if (f != null) {
        f.member() // unbox
    }

    if (f != null) {
        f.extensionOnNullable()
    }

    if (f != null) {
        val a = f
        a.member() // unbox
    }
}

// @TestKt.class:
// 0 INVOKESTATIC Foo\$Erased.box
// 2 INVOKEVIRTUAL Foo.unbox
// 0 valueOf
// 0 intValue