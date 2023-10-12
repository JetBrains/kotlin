// MODULE: m1-common
// FILE: common.kt
// TODO: .fir.kt version is just a stub.
<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect interface My {
    open fun openFunPositive()
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>open fun openFunNegative()<!>
    abstract fun abstractFun()

    open val openValPositive: Int
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>open val openValNegative: Int<!>
    abstract val abstractVal: Int
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_ERROR!>actual interface My<!> {
    actual fun openFunPositive() = Unit
    actual fun <!ACTUAL_WITHOUT_EXPECT, MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_ERROR!>openFunNegative<!>()
    actual fun abstractFun()

    actual val openValPositive: Int get() = 0
    actual val <!ACTUAL_WITHOUT_EXPECT, MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_ERROR!>openValNegative<!>: Int
    actual val abstractVal: Int
}
