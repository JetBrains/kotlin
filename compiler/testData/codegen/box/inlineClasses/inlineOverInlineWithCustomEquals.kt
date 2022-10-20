// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR


OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val x: Int) {
    fun equals(other: A) = true
}

class C

OPTIONAL_JVM_INLINE_ANNOTATION
value class B1(val x: A)

OPTIONAL_JVM_INLINE_ANNOTATION
value class B2(val x: A?)


OPTIONAL_JVM_INLINE_ANNOTATION
value class D1(val x: C) {
    fun equals(other: D1) = true
}


OPTIONAL_JVM_INLINE_ANNOTATION
value class D2(val x: C?) {
    fun equals(other: D2) = true
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class E1(val x: D1)

OPTIONAL_JVM_INLINE_ANNOTATION
value class E2(val x: D2)

OPTIONAL_JVM_INLINE_ANNOTATION
value class F<T>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class G<T : D1>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class H<T>(val x: F<T>)

fun box(): String {
    if (E1(D1(C())) != E1(D1(C()))) return "Fail 1"

    if (E2(D2(C())) != E2(D2(C()))) return "Fail 2.1"
    if (E2(D2(null)) != E2(D2(C()))) return "Fail 2.2"
    if (E2(D2(C())) != E2(D2(null))) return "Fail 2.3"
    if (E2(D2(null)) != E2(D2(null))) return "Fail 2.4"

    if (B1(A(0)) != B1(A(5))) return "Fail 3"

    if (B2(A(0)) != B2(A(5))) return "Fail 4.1"
    if (B2(null) == B2(A(5))) return "Fail 4.2"
    if (B2(A(0)) == B2(null)) return "Fail 4.3"
    if (B2(null) != B2(null)) return "Fail 4.4"

    if (F(D1(C())) != F(D1(C()))) return "Fail 5"

    if (G(D1(C())) != G(D1(C()))) return "Fail 6"

    if (H(F(1)) == H(F(2))) return "Fail 7"

    return "OK"
}