// FILE: test.kt
import kotlin.reflect.*

open class A {
    var x: String
    by
    B()
}

class B {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "OK"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        val name = property.name
    }
}

fun box() {
    val a = A()
    val x0 = a.x
    a.x = x0
}

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:21 box
// test.kt:4 <init>
// test.kt:7 <init>
// test.kt:10 <init>
// test.kt:7 <init>
// test.kt:4 <init>
// test.kt:21 box
// test.kt:22 box
// test.kt:6 getX
// test.kt:1 getX
// test.kt:6 getX
// test.kt:7 getX
// test.kt:12 getValue
// test.kt:7 getX
// test.kt:22 box
// test.kt:23 box
// test.kt:6 setX
// test.kt:1 setX
// test.kt:6 setX
// test.kt:7 setX
// test.kt:16 setValue
// test.kt:17 setValue
// test.kt:7 setX
// test.kt:24 box

// EXPECTATIONS FIR JVM_IR
// test.kt:21 box
// test.kt:4 <init>
// test.kt:7 <init>
// test.kt:10 <init>
// test.kt:7 <init>
// test.kt:4 <init>
// test.kt:21 box
// test.kt:22 box
// test.kt:7 getX
// test.kt:12 getValue
// test.kt:7 getX
// test.kt:22 box
// test.kt:23 box
// test.kt:7 setX
// test.kt:16 setValue
// test.kt:17 setValue
// test.kt:7 setX
// test.kt:24 box

// EXPECTATIONS JS_IR
// test.kt:21 box
// test.kt:7 <init>
// test.kt:10 <init>
// test.kt:4 <init>
// test.kt:22 box
// test.kt:7 <get-x>
// test.kt:7 <get-x>
// test.kt:7 <get-x>
// test.kt:12 getValue
// test.kt:23 box
// test.kt:7 <set-x>
// test.kt:7 <set-x>
// test.kt:7 <set-x>
// test.kt:16 setValue
// test.kt:17 setValue
// test.kt:24 box

// EXPECTATIONS WASM
// test.kt:21 $box (12)
// test.kt:7 $A.<init> (4)
// test.kt:18 $B.<init> (1)
// test.kt:7 $A.<init> (4)
// test.kt:8 $A.<init> (1)
// test.kt:22 $box (13, 15)
// test.kt:12 $B.getValue (15, 8)
// test.kt:22 $box (15)
// test.kt:23 $box (4, 10, 6)
// test.kt:16 $B.setValue (19, 28)
// test.kt:17 $B.setValue (5)
// test.kt:24 $box (1)
