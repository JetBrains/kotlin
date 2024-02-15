// MODULE: common
// TARGET_PLATFORM: Common
expect abstract class Foo() {
    abstract fun foo()
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Impl<!> : Foo() {}

fun common() {
    Impl().foo()
}

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
interface I {
    fun foo()
}

expect open class Base() {}

actual abstract class Foo : Base(), I {
    // In non-KMP world, these two f/o would squash into a single f/o final fun foo()
    // f/o abstract fun foo(): Unit in intermediate
    // f/o final fun foo(): Unit in platform
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}
