
// MODULE: common
// TARGET_PLATFORM: Common
expect open class A()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
class B : A() {
    fun foo(): String = "O"
}

fun getB(): B = B()

// MODULE: main()()(intermediate)
actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class A<!> actual constructor() {
    fun <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>bar<!>(): String = "K"
}

fun box(): String {
    val b = getB()
    return b.foo() + b.bar()
}
