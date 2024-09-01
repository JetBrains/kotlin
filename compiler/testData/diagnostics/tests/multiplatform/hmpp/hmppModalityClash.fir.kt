// MODULE: common
// TARGET_PLATFORM: Common
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect abstract class Foo() {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>abstract fun foo()<!>
}<!>

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED{METADATA}!>class Impl<!> : Foo() {}

fun common() {
    Impl().foo()
}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
interface I {
    fun foo()
}

expect open class Base() {}

actual abstract class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> : Base(), I {
    // In non-KMP world, these two f/o would squash into a single f/o final fun foo()
    // f/o abstract fun foo(): Unit in intermediate
    // f/o final fun foo(): Unit in platform
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}
