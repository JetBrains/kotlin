// PRE_RESOLVED_PHASE: STATUS
class Empty<caret>Cla<caret_preresolved>ss : InterfaceWithMembers {
    override val property: Int = 4
    override fun baseFunction() {}
}

interface InterfaceWithMembers : AnotherSuperInterface {
    val property: Int

    fun functionWithDefaultImplementation(i: Int): Int = i

    override fun baseFunction()

    override fun baz() {
        // default implementation
    }
}

interface AnotherSuperInterface {
    fun baz()

    fun baseFunction()
}
