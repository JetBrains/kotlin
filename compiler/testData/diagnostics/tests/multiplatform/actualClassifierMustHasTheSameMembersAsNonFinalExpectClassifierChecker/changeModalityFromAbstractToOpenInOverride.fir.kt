// MODULE: m1-common
// FILE: common.kt
interface Base {
    fun foo()
}
expect open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Foo<!>() : Base


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

// Mismatched scope must be reported here. But it's false negative checker in K1.
// For some reason, K1 says that modality of `exect_Foo.foo` is `abstract`.
// https://youtrack.jetbrains.com/issue/KT-59739
actual open class Foo : Base {
    override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
}
