// PRE_RESOLVED_PHASE: STATUS
interface EmptyIn<caret>ter<caret_preresolved>face : MarkerInterface1, MarkerInterface2, MarkerInterface3

interface MarkerInterface1
interface MarkerInterface2 : InterfaceWithMembers1
interface MarkerInterface3 : InterfaceWithMembers2

interface InterfaceWithMembers1 : AnotherSuperInterface {
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

interface InterfaceWithMembers2 {
    fun foo()
}