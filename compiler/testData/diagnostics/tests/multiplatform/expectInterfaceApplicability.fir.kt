// MODULE: m1-common
// FILE: common.kt
// TODO: .fir.kt version is just a stub.
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect interface My {
    open fun openFunPositive()
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>open fun openFunNegative()<!>
    abstract fun abstractFun()

    open val openValPositive: Int
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>open val openValNegative: Int<!>
    abstract val abstractVal: Int
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual interface My {
    actual fun openFunPositive() = Unit
    actual fun <!ACTUAL_WITHOUT_EXPECT!>openFunNegative<!>()
    actual fun abstractFun()

    actual val openValPositive: Int get() = 0
    actual val <!ACTUAL_WITHOUT_EXPECT!>openValNegative<!>: Int
    actual val abstractVal: Int
}
