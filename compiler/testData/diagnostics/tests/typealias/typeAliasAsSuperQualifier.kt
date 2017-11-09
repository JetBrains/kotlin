// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

open class Base {
    open fun foo() {}
}

open class GenericBase<T> {
    open fun foo() {}
}

class Unrelated {
    fun foo() {}
}

typealias B = Base
typealias U = Unrelated
typealias GB<T> = GenericBase<T>

class TestSuperForBase : B() {
    typealias MyBase = B

    override fun foo() {
        super<Base>.foo()
        super<B>.foo()
        super<MyBase>.foo()
        super<<!NOT_A_SUPERTYPE!>U<!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
    }
}

class TestSuperForGenericBase<T> : GB<T>() {
    typealias MyBase = GB<T>
    typealias MyBaseInt = GB<Int>

    override fun foo() {
        super<GenericBase>.foo()
        super<GB>.foo()
        super<MyBase>.foo()
        super<MyBaseInt>.foo() // Type arguments don't matter here
        super<<!NOT_A_SUPERTYPE!>U<!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
    }
}