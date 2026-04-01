// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: FULL
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full

fun mustUse(): String = ""

@IgnorableReturnValue
fun ignorable(): String = ""
