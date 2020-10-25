// JAVA_SOURCES: CovariantReturns.java

interface SubtypeKt : CovariantReturns {
    override fun makeObject(): Any
    override fun makeImplicitlyObjectBounded(): CovariantReturns.Lib<out Any>
    override fun makeExplicitlyObjectBounded(): CovariantReturns.Lib<out Any>
}
