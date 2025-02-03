// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY -UNSUPPORTED_FEATURE -DEBUG_INFO_MISSING_UNRESOLVED

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
        super<<!NOT_A_SUPERTYPE!>U<!>>.foo()
    }
}

typealias TopLevelMyBaseInt = GB<Int>

class TestSuperForGenericBase<T> : GB<T>() {
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias MyBase = GB<T>
    typealias MyBaseInt = GB<Int>

    override fun foo() {
        super<GenericBase>.foo()
        super<GB>.foo()
        super<MyBase>.foo()
        super<<!NOT_A_SUPERTYPE!>MyBaseInt<!>>.foo() // Type arguments don't matter here in K1 but matters in K2
        super<<!NOT_A_SUPERTYPE!>TopLevelMyBaseInt<!>>.foo() // because nested type aliases are treated as top-level type aliases
        super<<!NOT_A_SUPERTYPE!>U<!>>.foo()
    }
}
