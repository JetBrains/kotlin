// MODULE: common
// TARGET_PLATFORM: Common
expect class A() : B
expect class C() : B
expect open class B()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
actual class A : B() {
    // "Nothing to override" in metadata compilation. Unfortunately we don't check metadata compilation in diagnostic tests
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}
actual class C : B() {
    // Nothing to override in platform compilation.
    fun foo() {}
}

// MODULE: main()()(intermediate)
actual open class B {
    open fun foo() {}
}
