// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// FILE: test.kt
class Foo {
    inner class Bar {
    }
}

fun box() {
    val x = Foo()
    x.Bar()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:10 box:
// test.kt:4 <init>:
// test.kt:10 box:
// test.kt:11 box: x:Foo=Foo
// test.kt:5 <init>:
// test.kt:11 box: x:Foo=Foo
// test.kt:12 box: x:Foo=Foo

// EXPECTATIONS JS_IR
// test.kt:10 box:
// test.kt:4 <init>:
// test.kt:11 box: x=Foo
// test.kt:5 <init>:
// test.kt:5 <init>:
// test.kt:12 box: x=Foo
