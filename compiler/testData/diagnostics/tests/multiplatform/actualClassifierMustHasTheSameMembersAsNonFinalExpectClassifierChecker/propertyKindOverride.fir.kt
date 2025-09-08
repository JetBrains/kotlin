// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class Base {
    open val foo: Int = 1
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override var <!EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_KIND!>foo<!>: Int = 1
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, integerLiteral, override, propertyDeclaration */
