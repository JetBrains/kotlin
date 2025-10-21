// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: FULL
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full

fun mustUse(): String = ""

@IgnorableReturnValue
fun ignorable(): String = ""
