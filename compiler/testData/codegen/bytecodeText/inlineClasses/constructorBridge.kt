// !LANGUAGE: +InlineClasses
// FILE: test.kt
inline class A(val x: String)
class B(val y: A)

fun box() =
    B(A("OK")).y.x

// @TestKt.class:
// 1 INVOKESPECIAL B.<init> \(Ljava/lang/String;Lkotlin/jvm/internal/DefaultConstructorMarker;\)V