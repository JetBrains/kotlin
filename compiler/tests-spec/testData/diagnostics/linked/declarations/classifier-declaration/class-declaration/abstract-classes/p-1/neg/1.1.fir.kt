// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1

abstract class Base() {
    abstract fun foo() = {}
    fun boo() : Unit
    abstract val a = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>""<!>
    val b
    var d
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
