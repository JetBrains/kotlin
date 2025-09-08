// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
// TODO: .fir.kt version is just a stub.
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> interface My {
    open fun openFunPositive()
    open fun <!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>openFunNegative<!>()
    abstract fun abstractFun()

    open val openValPositive: Int
    open val <!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>openValNegative<!>: Int
    abstract val abstractVal: Int
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual interface My {
    actual fun openFunPositive() = Unit
    actual fun <!EXPECT_ACTUAL_INCOMPATIBLE_MODALITY!>openFunNegative<!>()
    actual fun abstractFun()

    actual val openValPositive: Int get() = 0
    actual val <!EXPECT_ACTUAL_INCOMPATIBLE_MODALITY!>openValNegative<!>: Int
    actual val abstractVal: Int
}

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
propertyDeclaration */
