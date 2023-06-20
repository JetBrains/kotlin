// ORIGINAL: /compiler/testData/diagnostics/tests/extensions/contextReceivers/noContextReceiversOnValueClasses.fir.kt
// !LANGUAGE: +ContextReceivers, +ValueClasses
// WITH_STDLIB
// SKIP_TXT
// WORKS_WHEN_VALUE_CLASS

@file:Suppress("INLINE_CLASS_DEPRECATED")

class A

context(A)
inline class B1(val x: Int)

context(A)
OPTIONAL_JVM_INLINE_ANNOTATION
value class B2(val x: Int)

context(A)
OPTIONAL_JVM_INLINE_ANNOTATION
value class C(val x: Int, val y: Int)

fun box() = "OK"
