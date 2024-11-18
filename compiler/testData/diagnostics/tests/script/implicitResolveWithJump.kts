// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: SCRIPT_CAPTURING_OBJECT: /implicitResolveWithJump.kts:5:1: error: object A captures the script class instance. Try to use class or anonymous object instead
// FIR_IDENTICAL
// DUMP_CFG: LEVELS

object A {
    val a = args[1]
}

val rv = A.a + B.b

object B {
    val b = args[2]
}
