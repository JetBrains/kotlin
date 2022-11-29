// FILE: test.kt

class A {
    val prop = 1

    fun foo() {
        prop
    }
}

fun box() {
    val a = A()
    a.prop
    a.foo()
}

// TODO: The JVM_IR backend has an extra line number on the return. This causes line
// three to be hit both on entry to the constructor and on exit after storing the
// value of prop.

// EXPECTATIONS JVM JVM_IR
// test.kt:12 box
// test.kt:3 <init>
// test.kt:4 <init>
// EXPECTATIONS JVM_IR
// test.kt:3 <init>
// EXPECTATIONS JVM JVM_IR
// test.kt:12 box
// test.kt:13 box
// test.kt:4 getProp
// test.kt:13 box
// test.kt:14 box
// test.kt:7 foo
// test.kt:8 foo
// test.kt:15 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:4 <init>
// test.kt:3 <init>
// test.kt:14 box
// test.kt:8 foo
// test.kt:15 box