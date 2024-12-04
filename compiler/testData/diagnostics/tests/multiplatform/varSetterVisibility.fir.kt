// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> var v1: Boolean

expect var v2: Boolean
    internal set

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> var v3: Boolean
    internal set

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> open class C {
    var <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>foo<!>: Boolean
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> open class C2 {
    var <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>foo<!>: Boolean
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual var <!ACTUAL_WITHOUT_EXPECT!>v1<!>: Boolean = false
    private set

actual var v2: Boolean = false

actual var <!ACTUAL_WITHOUT_EXPECT!>v3<!>: Boolean = false
    private set

actual open class C {
    actual var <!ACTUAL_WITHOUT_EXPECT!>foo<!>: Boolean = false
        protected set
}

open class C2Typealias {
    var foo: Boolean = false
        protected set
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>C2<!> = C2Typealias
