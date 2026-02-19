import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { computeFoo() }
    val a = A()
    expectSuccess { computeBar(a) }
    expectSuccess { computeBaz() }
}
