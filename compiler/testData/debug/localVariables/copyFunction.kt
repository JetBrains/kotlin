//FILE: test.kt

data class someClass(val a: Double, val b: Double)

fun box() {
    val a = someClass(1.0, 2.0)
    val b = a.copy(b = 3.0)
}

// EXPECTATIONS JVM_IR
// test.kt:6 box:
// test.kt:3 <init>: a:double=1.0:double, b:double=2.0:double
// test.kt:6 box:
// test.kt:7 box: a:someClass=someClass
// test.kt:3 <init>: a:double=1.0:double, b:double=3.0:double
// test.kt:-1 copy: a:double=1.0:double, b:double=3.0:double
// test.kt:7 box: a:someClass=someClass
// test.kt:8 box: a:someClass=someClass, b:someClass=someClass

// EXPECTATIONS JS_IR
// test.kt:6 box:
// test.kt:3 <init>: a=1:number, b=2:number
// test.kt:3 <init>: a=1:number, b=2:number
// test.kt:3 <init>: a=1:number, b=2:number
// test.kt:7 box: a=someClass
// test.kt:1 copy$default: b=3:number
// test.kt:1 copy$default: a=1:number, b=3:number
// test.kt:1 copy: a=1:number, b=3:number
// test.kt:3 <init>: a=1:number, b=3:number
// test.kt:3 <init>: a=1:number, b=3:number
// test.kt:3 <init>: a=1:number, b=3:number
// test.kt:8 box: a=someClass, b=someClass
