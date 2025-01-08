// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

interface Foo {
    fun foo()
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>NonAbstractClass<!> : Foo {
    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS{JVM}!>abstract<!> fun bar()

    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS{JVM}!>abstract<!> val baz: Int

    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS{JVM}!>abstract<!> override fun foo()
}

expect abstract class <!NO_ACTUAL_FOR_EXPECT{JVM}!>AbstractClass<!> : Foo {
    abstract fun bar()

    abstract val baz: Int

    abstract override fun foo()
}

// MODULE: m1-jvm()()(m1-common)
