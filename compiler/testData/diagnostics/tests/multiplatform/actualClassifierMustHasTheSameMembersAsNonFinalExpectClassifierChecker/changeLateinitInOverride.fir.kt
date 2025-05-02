// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class Base {
    open var red1: String = ""
    open lateinit var red2: String
    open lateinit var green: String
}

expect open class Foo : Base {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override lateinit var <!EXPECT_ACTUAL_INCOMPATIBILITY_PROPERTY_LATEINIT_MODIFIER!>red1<!>: String
    override var <!EXPECT_ACTUAL_INCOMPATIBILITY_PROPERTY_LATEINIT_MODIFIER!>red2<!>: String = ""
    override lateinit var green: String
}
