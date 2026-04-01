import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { test1() }
    expectSuccess { test2() }
    expectSuccess { test3() }
    expectSuccess { test4() }
    expectSuccess { test5() }
    expectSuccess { test6() }
}
