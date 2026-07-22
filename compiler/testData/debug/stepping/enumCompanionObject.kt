// FILE: test.kt

enum class Foo {
    A;

    companion object {
        val a = A
    }
}

fun box() {
    Foo.A;
    Foo.a;
}

// EXPECTATIONS JVM_IR
// test.kt:12 box
// test.kt:4 <clinit>
// test.kt:7 <clinit>
// test.kt:13 box
// test.kt:7 getA
// test.kt:7 getA
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS NATIVE
// test.kt:12 box
// test.kt:3 $getEnumAt
// test.kt:9 $getEnumAt
// test.kt:12 box
// test.kt:13 box
// test.kt:6 <get-$companion>
// test.kt:1 <get-$companion>
// test.kt:8 <get-$companion>
// test.kt:13 box
// test.kt:7 <get-a>
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:4 static_init_5
// test.kt:3 <init>
// test.kt:7 <init>
// test.kt:6 <init>
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS WASM
// test.kt:12 $box (8)
// test.kt:4 $static_init (4)
// test.kt:9 $Foo.<init> (1)
// test.kt:4 $static_init (4)
// test.kt:7 $Companion.<init> (16)
// test.kt:4 $static_init (4)
// test.kt:7 $Companion.<init> (16)
// test.kt:8 $Companion.<init> (5)
// test.kt:4 $static_init (4)
// test.kt:7 $static_init (16)
// test.kt:12 $box (8)
// test.kt:4 $static_init (4)
// test.kt:13 $box (8)
// test.kt:14 $box (1)
