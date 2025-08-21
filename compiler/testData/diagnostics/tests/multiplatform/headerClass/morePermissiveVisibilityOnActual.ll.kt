// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect open class Container {
    fun publicFun()

    internal fun internalFun1()
    internal fun internalFun2()
    internal fun internalFun3()

    protected fun protectedFun1()
    protected fun protectedFun2()
    protected fun protectedFun3()

    open internal fun openInternalFun()
    open fun openPublicFun()
}

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual open class Container {
    actual fun publicFun() {}               // OK: public -> public

    actual fun internalFun1() {}            // OK: internal -> public
    actual internal fun internalFun2() {}   // OK: internal -> internal

    actual fun protectedFun1() {}           // OK: protected -> public
    actual protected fun protectedFun2() {} // OK: protected -> protected

    actual internal fun <!EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY!>protectedFun3<!>() {}  // BAD: protected -> internal
    actual protected fun <!EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY!>internalFun3<!>() {}  // BAD: internal -> protected

    actual open fun <!EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY!>openInternalFun<!>() {}    // BAD: internal+open -> public
    actual internal fun <!EXPECT_ACTUAL_INCOMPATIBLE_MODALITY, EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY!>openPublicFun<!>() {}  // BAD: open+public -> internal
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration */
