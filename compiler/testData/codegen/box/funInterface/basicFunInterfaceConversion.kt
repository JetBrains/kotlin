// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions
// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_DCE_DRIVEN

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f.invoke()

fun box(): String {
    return foo { "OK" }
}