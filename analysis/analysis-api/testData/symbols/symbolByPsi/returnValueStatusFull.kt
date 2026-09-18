// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: FULL
// COMPILER_ARGUMENTS: -return-value-checker=full

fun mustUse(): String = ""

@IgnorableReturnValue
fun ignorable(): String = ""
