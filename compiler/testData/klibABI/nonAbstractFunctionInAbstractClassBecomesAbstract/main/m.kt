import abitestutils.abiTest
import lib1.A
import lib2.B
import lib2.B1
import lib2.B2

fun box() = abiTest {
    val a: A = B()
    val b = B()
    expectFailure(nonImplementedCallable("function 'foo'", "class 'B'")) { a.foo() }
    expectFailure(nonImplementedCallable("function 'bar'", "class 'B'")) { a.bar() }
    expectSuccess(-42) { a.baz() }
    expectFailure(nonImplementedCallable("function 'foo'", "class 'B'")) { b.unlinkedFunctionUsage }
    expectFailure(nonImplementedCallable("function 'foo'", "class 'B1'")) { B1() }
    expectFailure(nonImplementedCallable("function 'bar'", "class 'B2'")) { B2() }
}
