// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}!>class Foo<!>() {
    fun foo()
}

// MODULE: intermediate()()(common)
expect open class Base() {}

actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class Foo<!> : Base() {
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}
