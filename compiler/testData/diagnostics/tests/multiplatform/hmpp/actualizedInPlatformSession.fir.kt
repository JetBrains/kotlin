// MODULE: common
// TARGET_PLATFORM: Common
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo() {
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>fun foo()<!>
}<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
expect open class Base() {}

actual class Foo : Base() {
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}
