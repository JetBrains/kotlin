// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class Base() {
    open fun overrideReturnType(): Any = ""
    open fun overrideModality1(): Any = ""
    open fun overrideModality2(): Any = ""
    protected open fun overrideVisibility(): Any = ""
}

expect open class Foo : Base {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    fun injectedMethod() {}
    val injectedProperty: Int = 42
    override fun <!EXPECT_ACTUAL_INCOMPATIBILITY_RETURN_TYPE!>overrideReturnType<!>(): String = ""
    final override fun <!EXPECT_ACTUAL_INCOMPATIBILITY_MODALITY!>overrideModality1<!>(): Any = ""
    final override fun <!EXPECT_ACTUAL_INCOMPATIBILITY_MODALITY!>overrideModality2<!>(): Any = ""
    public override fun <!EXPECT_ACTUAL_INCOMPATIBILITY_VISIBILITY!>overrideVisibility<!>(): Any = ""
}
