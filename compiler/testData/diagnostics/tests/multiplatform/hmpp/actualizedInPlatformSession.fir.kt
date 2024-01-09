// MODULE: common
// TARGET_PLATFORM: Common
expect class Foo() {
    fun foo()
}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
expect open class Base() {}

actual class Foo : Base() {
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}
