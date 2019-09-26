package rawTypeskt11831

fun main(args: Array<String>) {
    val raw = forTests.MyJavaClass.RawADerived()
    val foo = raw.foo(emptyList<String>())
    //Breakpoint!
    val a = foo
}

// EXPRESSION: foo
// RESULT: 1: I