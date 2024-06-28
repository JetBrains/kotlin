// FIR_IDENTICAL
// WITH_STDLIB
// LANGUAGE: +ValueClasses

@file:Suppress("INLINE_CLASS_DEPRECATED")

inline class A1(val x: Int)

@JvmInline
value class A2(val x: Int)

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
inline class A3(val x: Int)

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class A4(val x: Int)


inline class B1(val x: Int, val y: Int)

@JvmInline
value class B2(val x: Int, val y: Int)

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
inline class B3(val x: Int, val y: Int)

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class B4(val x: Int, val y: Int)


inline class C1(val x: B2)

@JvmInline
value class C2(val x: B2)

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
inline class C3(val x: B2)

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class C4(val x: B2)
