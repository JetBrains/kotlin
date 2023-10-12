// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect open class Container {
    fun publicFun()

    internal fun internalFun1()
    internal fun internalFun2()
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>internal fun internalFun3()<!>

    protected fun protectedFun1()
    protected fun protectedFun2()
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>protected fun protectedFun3()<!>

    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>open internal fun openInternalFun()<!>
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>open fun openPublicFun()<!>
}<!>

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_ERROR!>class Container<!> {
    actual fun publicFun() {}               // OK: public -> public

    actual fun internalFun1() {}            // OK: internal -> public
    actual internal fun internalFun2() {}   // OK: internal -> internal

    actual fun protectedFun1() {}           // OK: protected -> public
    actual protected fun protectedFun2() {} // OK: protected -> protected

    actual <!VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_ERROR!>internal<!> fun <!ACTUAL_WITHOUT_EXPECT!>protectedFun3<!>() {}  // BAD: protected -> internal
    actual <!VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_ERROR!>protected<!> fun <!ACTUAL_WITHOUT_EXPECT!>internalFun3<!>() {}  // BAD: internal -> protected

    actual open fun <!ACTUAL_WITHOUT_EXPECT, VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_ERROR!>openInternalFun<!>() {}    // BAD: internal+open -> public
    actual internal fun <!ACTUAL_WITHOUT_EXPECT, MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_ERROR!>openPublicFun<!>() {}  // BAD: open+public -> internal
}
