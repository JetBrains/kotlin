// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class Base {
    open var foo: String = ""
        protected set
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override var <!EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY!>foo<!>: String = ""
        public set
}
