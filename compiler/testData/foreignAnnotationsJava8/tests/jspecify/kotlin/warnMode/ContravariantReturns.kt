// JAVA_SOURCES: ContravariantReturns.java

interface SubtypeKt : ContravariantReturns {
    // jspecify_nullness_mismatch
    override fun makeObject(): Any?
    // jspecify_nullness_mismatch
    override fun makeImplicitlyObjectBounded(): ContravariantReturns.Lib<*>
    // jspecify_nullness_mismatch
    override fun makeExplicitlyObjectBounded(): ContravariantReturns.Lib<out Any?>
}
