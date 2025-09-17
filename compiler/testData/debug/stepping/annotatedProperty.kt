// FILE: test.kt

@Target(AnnotationTarget.PROPERTY)
annotation class Anno

class C {
    @Anno
    var x: Any = 1

    @Anno
    lateinit var y: Any
}

fun box() {
    val c = C()

    c.x = 2
    c.x

    c.y = 2
    c.y
}

// EXPECTATIONS JVM_IR
// test.kt:15 box
// test.kt:6 <init>
// test.kt:8 <init>
// test.kt:6 <init>
// test.kt:15 box
// test.kt:17 box
// test.kt:8 setX
// test.kt:18 box
// test.kt:8 getX
// test.kt:18 box
// test.kt:20 box
// test.kt:11 setY
// test.kt:21 box
// test.kt:11 getY
// test.kt:21 box
// test.kt:22 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:8 <init>
// test.kt:6 <init>
// test.kt:17 box
// test.kt:20 box
// test.kt:21 box
// test.kt:11 <get-y>
// test.kt:11 <get-y>
// test.kt:11 <get-y>
// test.kt:22 box

// EXPECTATIONS WASM
// test.kt:15 $box (12)
// test.kt:8 $C.<init> (17)
// test.kt:12 $C.<init> (1)
// test.kt:17 $box (4, 10, 6)
// test.kt:18 $box (4, 6)
// test.kt:20 $box (4, 10, 6)
// test.kt:21 $box (4, 6)
// test.kt:22 $box (1)
