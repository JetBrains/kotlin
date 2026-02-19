// PRE_RESOLVED_PHASE: STATUS
class EmptyC<caret>la<caret_preresolved>ss : InterfaceWithMembers

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
