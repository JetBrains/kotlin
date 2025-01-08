// RUN_PIPELINE_TILL: FIR2IR
// DUMP_CFG: LEVELS

<!SCRIPT_CAPTURING_OBJECT!>object A<!> {
    val a = args[1]
}

val rv = A.a + B.b

<!SCRIPT_CAPTURING_OBJECT!>object B<!> {
    val b = args[2]
}
