// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1

abstract class Base() {
    <!ABSTRACT_FUNCTION_WITH_BODY!>abstract<!> fun foo() = {}
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun boo() : Unit<!>
    abstract val a = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>""<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val b<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var d<!>
}

class Impl : Base() {
    override fun foo(): () -> Unit {
        TODO("not implemented")
    }

    override val a: String
        get() = TODO("not implemented")

}

fun case1() {
    val impl = Impl()
}
