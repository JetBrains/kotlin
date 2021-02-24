// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions
// !DIAGNOSTICS: -UNUSED_PARAMETER

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
    <!INAPPLICABLE_CANDIDATE!>takeWithoutFun<!>({})
    takeWithFun {}
}
