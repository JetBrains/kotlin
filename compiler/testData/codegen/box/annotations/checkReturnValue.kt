// LANGUAGE: +UnnamedLocalVariables
// RETURN_VALUE_CHECKER_MODE: FULL
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ KT-80605: MustUseReturnValues is added since vesrion 2.3

@file:MustUseReturnValues

fun mustUseReturnValueFun(): Int = 123

fun useReturnValueFun(a: Boolean): Int {
    val result = mustUseReturnValueFun()
    return if (a) mustUseReturnValueFun() else mustUseReturnValueFun()
}

@IgnorableReturnValue
fun ignorableReturnValueFun(): Int = 456

fun ignoreReturnValue1() {
    ignorableReturnValueFun()
}

fun explicitlyIgnoredReturnValueFun(): Int = 789

fun ignoreReturnValue2() {
    val _ = explicitlyIgnoredReturnValueFun()
}

fun box(): String {
    if (useReturnValueFun(true) != 123) return "FAIL1"
    if (useReturnValueFun(false) != 123) return "FAIL2"
    ignoreReturnValue1()
    ignoreReturnValue2()
    return "OK"
}
