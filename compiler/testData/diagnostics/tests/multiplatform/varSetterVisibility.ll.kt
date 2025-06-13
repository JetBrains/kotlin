// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect var v1: Boolean

expect var v2: Boolean
    internal set

expect var v3: Boolean
    internal set

expect open class C {
    var foo: Boolean
}

expect open class C2 {
    var foo: Boolean
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual var <!EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY!>v1<!>: Boolean = false
    private set

actual var v2: Boolean = false

actual var <!EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY!>v3<!>: Boolean = false
    private set

actual open class C {
    actual var <!EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY!>foo<!>: Boolean = false
        protected set
}

open class C2Typealias {
    var foo: Boolean = false
        protected set
}

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>C2<!> = C2Typealias

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, propertyDeclaration, typeAliasDeclaration */
