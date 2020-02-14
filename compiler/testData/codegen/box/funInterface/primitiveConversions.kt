// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_DCE_DRIVEN

// This test should check argument coercion between the SAM and the lambda.
// For now it checks that Char is boxed in JS

fun interface CharToAny {
    fun invoke(c: Char): Any
}

fun foo(c: CharToAny): Any = c.invoke('O')

fun box(): String {

    if (foo { it } !is Char) return "fail"

    return "OK"
}