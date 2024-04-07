// MODULE: common
// TARGET_PLATFORM: Common
expect class Foo() {
    fun foo()
}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
expect open class Base() {}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> : Base() {
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}
