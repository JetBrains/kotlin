// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Container {
    fun publicFun()

    internal fun internalFun1()
    internal fun internalFun2()
    <!INCOMPATIBLE_MATCHING{JVM}!>internal fun internalFun3()<!>

    protected fun protectedFun1()
    protected fun protectedFun2()
    <!INCOMPATIBLE_MATCHING{JVM}!>protected fun protectedFun3()<!>

    <!INCOMPATIBLE_MATCHING{JVM}!>open internal fun openInternalFun()<!>
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun openPublicFun()<!>
}<!>

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual open class Container {
    actual fun publicFun() {}               // OK: public -> public

    actual fun internalFun1() {}            // OK: internal -> public
    actual internal fun internalFun2() {}   // OK: internal -> internal

    actual fun protectedFun1() {}           // OK: protected -> public
    actual protected fun protectedFun2() {} // OK: protected -> protected

    <!ACTUAL_WITHOUT_EXPECT!>actual internal fun protectedFun3() {}<!>  // BAD: protected -> internal
    <!ACTUAL_WITHOUT_EXPECT!>actual protected fun internalFun3() {}<!>  // BAD: internal -> protected

    <!ACTUAL_WITHOUT_EXPECT!>actual open fun openInternalFun() {}<!>    // BAD: internal+open -> public
    <!ACTUAL_WITHOUT_EXPECT!>actual internal fun openPublicFun() {}<!>  // BAD: open+public -> internal
}
