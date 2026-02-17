import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { test1() }
    expectSuccess { test2() }
    expectSuccess { test3() }
}
