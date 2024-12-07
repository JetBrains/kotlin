// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> open class Container {
    fun publicFun()

    internal fun internalFun1()
    internal fun internalFun2()
    internal fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>internalFun3<!>()

    protected fun protectedFun1()
    protected fun protectedFun2()
    protected fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>protectedFun3<!>()

    open internal fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>openInternalFun<!>()
    open fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>openPublicFun<!>()
}

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual open class Container {
    actual fun publicFun() {}               // OK: public -> public

    actual fun internalFun1() {}            // OK: internal -> public
    actual internal fun internalFun2() {}   // OK: internal -> internal

    actual fun protectedFun1() {}           // OK: protected -> public
    actual protected fun protectedFun2() {} // OK: protected -> protected

    actual internal fun <!ACTUAL_WITHOUT_EXPECT!>protectedFun3<!>() {}  // BAD: protected -> internal
    actual protected fun <!ACTUAL_WITHOUT_EXPECT!>internalFun3<!>() {}  // BAD: internal -> protected

    actual open fun <!ACTUAL_WITHOUT_EXPECT!>openInternalFun<!>() {}    // BAD: internal+open -> public
    actual internal fun <!ACTUAL_WITHOUT_EXPECT!>openPublicFun<!>() {}  // BAD: open+public -> internal
}
