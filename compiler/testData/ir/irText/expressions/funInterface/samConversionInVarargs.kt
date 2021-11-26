fun interface IFoo {
    fun foo(i: Int)
}

fun useVararg(vararg foos: IFoo) {}

fun testLambda() {
    useVararg({})
}

fun testSeveralLambdas() {
    useVararg({}, {}, {})
}

fun withVarargOfInt(vararg xs: Int) = ""

fun testAdaptedCR() {
    useVararg(::withVarargOfInt)
}
