import abitestutils.abiTest
import lib1.A
import lib2.B

fun box() = abiTest {
    val a: A = B()
    expectFailure(nonImplementedCallable("property accessor 'foo.<get-foo>'", "class 'B'")) { a.foo }
}
