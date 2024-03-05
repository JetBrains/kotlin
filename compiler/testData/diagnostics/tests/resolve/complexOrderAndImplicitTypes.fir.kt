// ISSUE: KT-66313

val foo: String get() = ""

class Test1 {
    private val otherFoo = foo

    fun getFoo() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>otherFoo<!>
}

class Test2 {
    fun getFoo() = otherFoo

    private val otherFoo = foo
}
