// FILE: test.kt
class Foo {
    inner class Bar {
    }
}

fun box() {
    val x = Foo()
    x.Bar()
}

// EXPECTATIONS JVM_IR
// test.kt:8 box:
// test.kt:2 <init>:
// test.kt:8 box:
// test.kt:9 box: x:Foo=Foo
// test.kt:3 <init>:
// test.kt:9 box: x:Foo=Foo
// test.kt:10 box: x:Foo=Foo

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:2 <init>:
// test.kt:9 box: x=Foo
// test.kt:3 <init>:
// test.kt:3 <init>:
// test.kt:10 box: x=Foo

// EXPECTATIONS WASM
// test.kt:8 $box: $x:(ref null $Foo)=null (12, 12)
// test.kt:5 $Foo.<init>: $<this>:(ref $Foo)=(ref $Foo) (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:8 $box: $x:(ref null $Foo)=null (12)
// test.kt:9 $box: $x:(ref $Foo)=(ref $Foo) (6, 4, 6)
// test.kt:3 $Bar.<init>: $<this>:(ref $Bar)=(ref $Bar), $$outer:(ref $Foo)=(ref $Foo) (4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:4 $Bar.<init>: $<this>:(ref $Bar)=(ref $Bar), $$outer:(ref $Foo)=(ref $Foo) (5, 5, 5)
// test.kt:9 $box: $x:(ref $Foo)=(ref $Foo) (6)
// test.kt:10 $box: $x:(ref $Foo)=(ref $Foo) (1, 1)
