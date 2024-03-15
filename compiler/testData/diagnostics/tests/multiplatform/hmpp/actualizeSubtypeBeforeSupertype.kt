// MODULE: common
// TARGET_PLATFORM: Common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}!>class A<!>() : B
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}!>class C<!>() : B
expect open class B()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class A<!> : B() {
    // "Nothing to override" in metadata compilation. Unfortunately we don't check metadata compilation in diagnostic tests
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class C<!> : B() {
    // Nothing to override in platform compilation.
    fun <!VIRTUAL_MEMBER_HIDDEN{JVM}!>foo<!>() {}
}

// MODULE: main()()(intermediate)
actual open class B {
    open fun foo() {}
}
