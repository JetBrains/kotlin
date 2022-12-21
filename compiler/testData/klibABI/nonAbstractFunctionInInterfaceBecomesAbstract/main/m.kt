import abitestutils.abiTest
import lib1.A
import lib2.B
import lib2.B1

fun box() = abiTest {
    val a: A = B()
    val b = B()
    expectFailure(nonImplementedCallable("function 'foo'", "class 'B'")) { a.foo() }
    expectSuccess(-42) { a.bar() }
    expectFailure(nonImplementedCallable("function 'foo'", "class 'B'")) { b.unlinkedFunctionUsage }
    expectFailure(nonImplementedCallable("function 'foo'", "class 'B1'")) { B1() }
}
