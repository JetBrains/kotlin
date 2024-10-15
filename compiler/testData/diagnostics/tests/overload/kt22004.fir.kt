// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing error: Ignoring an implementation of the method `void A.b()` because it has multiple definitions
// ISSUE: KT-22004

class A() {
    fun b() {
    }

    @Deprecated("test", level = DeprecationLevel.HIDDEN)
    fun b() {
    }
}
