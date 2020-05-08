// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions
// SKIP_DCE_DRIVEN

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f.invoke()

fun box(): String {
    return foo { "OK" }
}