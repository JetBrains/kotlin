// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: CHECKER
// COMPILER_ARGUMENTS: -Xreturn-value-checker=check

@MustUseReturnValues
class Marked {
    fun mustUse(): String = ""

    @IgnorableReturnValue
    fun ignorable(): String = ""
}

fun unspecified(): String = ""