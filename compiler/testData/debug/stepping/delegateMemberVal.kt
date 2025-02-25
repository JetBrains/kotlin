// FILE: test.kt
class A {
    val z
    by
    Delegate {
        23
    }
}

class Delegate(
    val f: () -> Int
) {
    operator fun getValue(thisRef: Any?, property: Any): Int {
        return f()
    }
}

fun box() {
    val z0 = A().z
}

// EXPECTATIONS JVM_IR
// test.kt:19 box
// EXPECTATIONS FIR JVM_IR
// test.kt:5 <clinit>
// test.kt:5 <clinit>
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:4 <clinit>
// test.kt:4 <clinit>
// EXPECTATIONS JVM_IR
// test.kt:19 box
// test.kt:2 <init>
// test.kt:5 <init>
// test.kt:10 <init>
// test.kt:11 <init>
// test.kt:10 <init>
// test.kt:5 <init>
// test.kt:2 <init>
// test.kt:19 box
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:4 getZ
// test.kt:1 getZ
// test.kt:4 getZ
// EXPECTATIONS JVM_IR
// test.kt:5 getZ
// test.kt:14 getValue
// EXPECTATIONS FIR JVM_IR
// test.kt:6 z_delegate$lambda$0
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:6 invoke
// EXPECTATIONS JVM_IR
// test.kt:14 getValue
// test.kt:5 getZ
// test.kt:19 box
// test.kt:20 box

// EXPECTATIONS JS_IR
// test.kt:19 box
// test.kt:5 <init>
// test.kt:5 <init>
// test.kt:11 <init>
// test.kt:10 <init>
// test.kt:2 <init>
// test.kt:19 box
// EXPECTATIONS ClassicFrontend JS_IR
// test.kt:4 <get-z>
// test.kt:4 z$factory
// EXPECTATIONS FIR JS_IR
// test.kt:5 <get-z>
// test.kt:5 z$factory
// EXPECTATIONS JS_IR
// test.kt:5 <get-z>
// test.kt:14 getValue
// test.kt:6 A$z$delegate$lambda
// test.kt:20 box

// EXPECTATIONS WASM
// test.kt:19 $box (13)
// test.kt:5 $A.<init> (4)
// test.kt:11 $Delegate.<init> (4)
// test.kt:12 $Delegate.<init> (1)
// test.kt:5 $A.<init> (4)
// test.kt:8 $A.<init> (1)
// test.kt:19 $box (17)
// test.kt:14 $Delegate.getValue (15)
// test.kt:6 $A$z$delegate$lambda.invoke (8, 10)
// test.kt:14 $Delegate.getValue (15, 8)
// test.kt:19 $box (17)
// test.kt:20 $box (1)
