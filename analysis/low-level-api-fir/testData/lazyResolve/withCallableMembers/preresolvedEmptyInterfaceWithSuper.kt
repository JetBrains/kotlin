// PRE_RESOLVED_PHASE: STATUS
interface EmptyI<caret>nte<caret_preresolved>rface : InterfaceWithMembers

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
