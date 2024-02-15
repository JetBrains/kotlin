// MODULE: common
// TARGET_PLATFORM: Common
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class A() : B<!>
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class C() : B<!>
expect open class B()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
actual class A : B() {
    // "Nothing to override" in metadata compilation. Unfortunately we don't check metadata compilation in diagnostic tests
    override fun foo() {}
}
actual class C : B() {
    // Nothing to override in platform compilation.
    fun <!VIRTUAL_MEMBER_HIDDEN!>foo<!>() {}
}

// MODULE: main()()(intermediate)
actual open class B {
    open fun foo() {}
}
