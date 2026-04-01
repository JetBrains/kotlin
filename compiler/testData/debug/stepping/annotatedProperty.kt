// FILE: test.kt

@Target(AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.FIELD,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER)
annotation class Anno

class C {
    @Anno
    var x: Any = 1

    @Anno
    lateinit var y: Any

    @field:Anno
    @get:Anno
    @set:Anno
    var z: Any = 1
}

fun box() {
    val c = C()

    c.x = 2
    c.x

    c.y = 2
    c.y

    c.z = 2
    c.z
}

// EXPECTATIONS JVM_IR
// test.kt:23 box
// test.kt:9 <init>
// test.kt:11 <init>
// test.kt:19 <init>
// test.kt:9 <init>
// test.kt:23 box
// test.kt:25 box
// test.kt:11 setX
// test.kt:26 box
// test.kt:11 getX
// test.kt:26 box
// test.kt:28 box
// test.kt:14 setY
// test.kt:29 box
// test.kt:14 getY
// test.kt:29 box
// test.kt:31 box
// test.kt:19 setZ
// test.kt:32 box
// test.kt:19 getZ
// test.kt:32 box
// test.kt:33 box

// EXPECTATIONS NATIVE
// test.kt:23 box
// test.kt:9 <init>
// test.kt:11 <init>
// test.kt:19 <init>
// test.kt:20 <init>
// test.kt:23 box
// test.kt:25 box
// test.kt:11 <set-x>
// test.kt:25 box
// test.kt:26 box
// test.kt:11 <get-x>
// test.kt:26 box
// test.kt:28 box
// test.kt:14 <set-y>
// test.kt:28 box
// test.kt:29 box
// test.kt:14 <get-y>
// test.kt:29 box
// test.kt:31 box
// test.kt:19 <set-z>
// test.kt:31 box
// test.kt:32 box
// test.kt:19 <get-z>
// test.kt:33 box

// EXPECTATIONS JS_IR
// test.kt:23 box
// test.kt:11 <init>
// test.kt:19 <init>
// test.kt:9 <init>
// test.kt:25 box
// test.kt:28 box
// test.kt:29 box
// test.kt:14 <get-y>
// test.kt:14 <get-y>
// test.kt:14 <get-y>
// test.kt:31 box
// test.kt:33 box

// EXPECTATIONS WASM
// test.kt:23 $box (12)
// test.kt:11 $C.<init> (17)
// test.kt:19 $C.<init> (17)
// test.kt:20 $C.<init> (1)
// test.kt:25 $box (4, 10, 6)
// test.kt:26 $box (4, 6)
// test.kt:28 $box (4, 10, 6)
// test.kt:29 $box (4, 6)
// test.kt:31 $box (4, 10, 6)
// test.kt:32 $box (4, 6)
// test.kt:33 $box (1)
