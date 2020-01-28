// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType +FunctionalInterfaceConversion +SamConversionPerArgument +SamConversionForKotlinFunctions

fun interface IFoo {
    fun foo(i: Int)
}

fun useFoo(foo: IFoo) {}

fun useVarargFoo(vararg foos: IFoo) {}

fun withVararg(vararg xs: Int) = 42

fun test() {
    useFoo(::withVararg)
}

// TODO
//fun testVarargOfSams() {
//    useVarargFoo(::withVararg)
//}