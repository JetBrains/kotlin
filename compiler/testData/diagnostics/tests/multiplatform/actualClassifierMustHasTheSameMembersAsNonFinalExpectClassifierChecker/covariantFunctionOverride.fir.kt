// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class Base {
    open val foo: String = ""
    open fun foo(): Any = ""
}

expect open class Foo : Base {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override fun <!EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE!>foo<!>(): String = ""
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, override, propertyDeclaration,
stringLiteral */
