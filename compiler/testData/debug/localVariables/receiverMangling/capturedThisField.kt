

// FILE: test.kt
class Foo {
    inner class Bar {
    }
}

fun box() {
    val x = Foo()
    x.Bar()
}

// LOCAL VARIABLES
// test.kt:10 box:
// test.kt:4 <init>:
// test.kt:10 box:
// test.kt:11 box: x:Foo=Foo
// test.kt:5 <init>:
// test.kt:11 box: x:Foo=Foo
// test.kt:12 box: x:Foo=Foo