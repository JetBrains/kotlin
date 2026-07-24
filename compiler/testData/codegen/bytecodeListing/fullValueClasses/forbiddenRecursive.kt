// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_LISTING
// WORKS_WHEN_VALUE_CLASS
// WITH_STDLIB

@file:Suppress("VALUE_CLASS_CANNOT_BE_RECURSIVE")

value class A(val y: Int, val z: A)
value class B(val y: Int, val z: B?)

value class C(val a: C?, val b: C, val c: C, val d: C?, val e: D)

value class D(val a: C)
value class E<T>(val a: T)
value class F<T>(val a: T?)

fun C.wrap() = C(null, this, this, this, D(this))
inline fun <T> E<T>.wrap() = E(this)
inline fun <T> F<T>.wrap() = F(this)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Old1(val x: New1)

value class New1(val x: Old1)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Old2(val x: New2)

value class New2(val x: New2)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Old5(val x: New5)

value class New5(val x: Old5, val y: Old5)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Old6(val x: New6)

value class New6(val x: New6, val y: New6)

value class New7(val x: New7_)

value class New7_(val x: New7, val y: New7)
