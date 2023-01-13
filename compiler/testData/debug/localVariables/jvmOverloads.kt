// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt
class C {
    @kotlin.jvm.JvmOverloads fun foo(firstParam: Int, secondParam: String = "") {
    }
}

fun box() {
    C().foo(4)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:10 box:
// test.kt:4 <init>:
// test.kt:10 box:
// test.kt:6 foo: firstParam:int=4:int, secondParam:java.lang.String="":java.lang.String
// test.kt:11 box:
