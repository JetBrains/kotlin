import abitestutils.abiTest
import lib.C

fun box() = abiTest {
    val c = C()
    expectSuccess(42) { c.f() }
    expectSuccess(2) { c.p1 }
    expectSuccess(-1) { c.p2 }
    expectSuccess(-84) { c.compute() }
}
