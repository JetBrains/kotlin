// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing error: Ignoring an implementation of the method `int Aaa.getA()` because it has multiple definitions
class Aaa() {
    val <!REDECLARATION!>a<!> = 1
    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    val <!REDECLARATION!>a<!> = 1
}