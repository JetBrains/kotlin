// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

fun interface IFoo {
    fun foo(i: Int)
}

fun useFoo(foo: IFoo) {}

fun withVararg(vararg xs: Int) = 42

fun test() {
    useFoo(::withVararg)
}

