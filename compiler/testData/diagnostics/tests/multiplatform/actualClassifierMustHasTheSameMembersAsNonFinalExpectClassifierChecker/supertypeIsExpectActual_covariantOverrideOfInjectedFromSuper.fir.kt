// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

expect open class Base {
    fun existingMethodInBase()
}

expect open class Foo : Base {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@OptIn(ExperimentalMultiplatform::class)
@AllowDifferentMembersInActual
actual open class Base {
    actual fun existingMethodInBase() {}
    open fun injected(): Any = ""
}

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    // K1 and K2 see the world differently (K1 sees actuals when it resolves expect supertypes) => they compare the scopes differently.
    // It's ok because using AllowDifferentMembersInActual is undefined behavior
    override fun <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>injected<!>(): String = "" // covariant override
}
