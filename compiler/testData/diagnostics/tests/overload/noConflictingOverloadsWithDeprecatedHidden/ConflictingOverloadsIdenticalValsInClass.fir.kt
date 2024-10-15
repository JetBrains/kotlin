// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing error: Ignoring an implementation of the method `int Aaa.getA()` because it has multiple definitions
class Aaa() {
    val a = 1
    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    val a = 1
}