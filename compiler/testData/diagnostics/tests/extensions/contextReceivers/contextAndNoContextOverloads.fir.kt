// LANGUAGE: +ContextReceivers
// ISSUE: KT-52002

class Scope(val name: String)

interface Interface {
    fun foo()

    context(Scope)
    fun foo()
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ClassNoContext<!> : Interface {
    override fun foo() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ClassContext<!> : Interface {
    context(Scope)
    override fun foo() {}
}

class ClassBoth : Interface {
    override fun foo() {}

    context(Scope)
    override fun foo() {}
}

fun test() {
    val scope = Scope("")
    val c = ClassBoth()
    c.foo()
    with(scope) { c.foo() }
}
