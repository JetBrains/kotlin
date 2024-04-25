// LANGUAGE: +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions
// DIAGNOSTICS: -UNUSED_PARAMETER

fun interface Base {
    fun invoke()
}

interface WithoutFun : Base
fun interface WithFun : Base

fun takeBase(b: Base) {}
fun takeWithoutFun(a: WithoutFun) {}
fun takeWithFun(a: WithFun) {}

fun test() {
    takeBase {}
    takeWithoutFun(<!ARGUMENT_TYPE_MISMATCH!>{}<!>)
    takeWithFun {}
}
