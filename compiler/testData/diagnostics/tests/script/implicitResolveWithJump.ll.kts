// LL_FIR_DIVERGENCE
//   `SCRIPT_CAPTURING_OBJECT` is reported from the backend part of scripting plugin,
//   which is not invoked by AA
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FIR2IR
// DUMP_CFG: LEVELS

object A {
    val a = args[1]
}

val rv = A.a + B.b

object B {
    val b = args[2]
}
