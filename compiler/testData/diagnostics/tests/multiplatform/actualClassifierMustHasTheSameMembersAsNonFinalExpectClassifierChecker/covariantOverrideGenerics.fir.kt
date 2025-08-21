// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class Base<R> {
    open fun foo(): R = null!!
}

expect open class Foo<R, T : R> : Base<R> {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo<E, F : E> : Base<E>() {
    override fun <!EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE!>foo<!>(): F = null!!
}

/* GENERATED_FIR_TAGS: actual, checkNotNullCall, classDeclaration, expect, functionDeclaration, nullableType, override,
typeConstraint, typeParameter */
