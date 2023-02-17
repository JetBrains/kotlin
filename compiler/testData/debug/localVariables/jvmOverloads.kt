// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
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
// test.kt:12 box:
// test.kt:6 <init>:
// test.kt:12 box:
// test.kt:8 foo: firstParam:int=4:int, secondParam:java.lang.String="":java.lang.String
// test.kt:13 box:
