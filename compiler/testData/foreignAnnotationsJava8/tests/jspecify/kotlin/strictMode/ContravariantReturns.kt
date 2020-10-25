// JAVA_SOURCES: ContravariantReturns.java
// JSPECIFY_STATE strict

interface SubtypeKt : ContravariantReturns {
    // jspecify_nullness_mismatch
    override fun makeObject(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Any?<!>
    // jspecify_nullness_mismatch
    override fun makeImplicitlyObjectBounded(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>ContravariantReturns.Lib<*><!>
    // jspecify_nullness_mismatch
    override fun makeExplicitlyObjectBounded(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>ContravariantReturns.Lib<out Any?><!>
}
