// !LANGUAGE: +NewInference +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f.invoke()

fun test() = foo { "OK" }