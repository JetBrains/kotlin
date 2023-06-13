// MODULE: m1-common
// FILE: common.kt

interface Foo {
    fun foo()
}

<!NO_ACTUAL_FOR_EXPECT!>expect class NonAbstractClass : Foo {
    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS!>abstract<!> fun bar()

    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val baz: Int

    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS!>abstract<!> override fun foo()
}<!>

<!NO_ACTUAL_FOR_EXPECT!>expect abstract class AbstractClass : Foo {
    abstract fun bar()

    abstract val baz: Int

    abstract override fun foo()
}<!>
