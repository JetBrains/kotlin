// LANGUAGE: +ContextReceivers
// ISSUE: KT-52002

class Scope(val name: String)

interface Interface {
    fun foo()

    context(Scope)
    fun foo()
}

class ClassNoContext : Interface {
    override fun foo() {}
}

class ClassContext : Interface {
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
