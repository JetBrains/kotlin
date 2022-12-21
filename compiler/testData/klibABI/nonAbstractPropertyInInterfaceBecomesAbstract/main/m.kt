import abitestutils.abiTest
import lib1.A
import lib2.B
import lib2.B1

fun box() = abiTest {
    val a: A = B()
    val b = B()
    expectFailure(nonImplementedCallable("property accessor 'foo.<get-foo>'", "class 'B'")) { a.foo }
    expectSuccess(-42) { a.bar }
    expectFailure(nonImplementedCallable("property accessor 'foo.<get-foo>'", "class 'B'")) { b.unlinkedPropertyUsage }
    expectFailure(nonImplementedCallable("property accessor 'foo.<get-foo>'", "class 'B1'")) { B1() }
}
