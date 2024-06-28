// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>fun foo()<!> {}
class <!CLASSIFIER_REDECLARATION!>Foo<!>

open class Base {
    open fun foo() {}
}
expect class Bar : Base {
}

expect open class ExpectBase {
    open fun foo()
}
expect class Baz : ExpectBase

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
actual class <!ACTUAL_WITHOUT_EXPECT!>Foo<!>

actual class Bar : Base() {
    actual override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}

actual open class ExpectBase {
    actual open fun foo() {}
}
actual class Baz : ExpectBase() {
    actual override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}
