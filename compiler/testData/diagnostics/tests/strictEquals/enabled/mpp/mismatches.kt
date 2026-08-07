// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: commonFile.kt
package p

open class P1

expect class C1 : P1 {
    override fun equals(@EqualityBound(C1::class) other: Any?): Boolean
}

expect class C2 : P1 {
    override fun equals(@EqualityBound(P1::class) other: Any?): Boolean
}

expect class C3 : P1 {
    override fun equals(@EqualityBound(P1::class) other: Any?): Boolean
}

expect class C4 : P1 {
    override fun equals(other: Any?): Boolean
}

expect class C5 : P1 {
    override fun equals(@EqualityBound(P1::class) other: Any?): Boolean
}

expect class C6 : P1

expect class C7

// MODULE: platform()()(common)
// FILE: platformFile.kt
package p

actual class C1 : P1() {
    actual override fun <!EXPECT_ACTUAL_INCOMPATIBLE_EQUALITY_BOUNDS!>equals<!>(@EqualityBound(P1::class) other: Any?): Boolean = true
}

actual class C2 : P1() {
    actual override fun <!EXPECT_ACTUAL_INCOMPATIBLE_EQUALITY_BOUNDS!>equals<!>(@EqualityBound(C2::class) other: Any?): Boolean = true
}

actual class C3 : P1() {
    actual override fun <!EXPECT_ACTUAL_INCOMPATIBLE_EQUALITY_BOUNDS!>equals<!>(other: Any?): Boolean = other is C3
}

actual class C4 : P1() {
    actual override fun <!EXPECT_ACTUAL_INCOMPATIBLE_EQUALITY_BOUNDS!>equals<!>(@EqualityBound(C4::class) other: Any?): Boolean = true
}

actual class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>C5<!> : P1()

actual class C6 : P1() {
    override fun <!EXPECT_ACTUAL_INCOMPATIBLE_EQUALITY_BOUNDS!>equals<!>(@EqualityBound(Any::class) other: Any?): Boolean = other is C6
}

abstract class I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean = true
}

actual class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>C7<!> : I()

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration, isExpression, nullableType,
operator, override, smartcast */
