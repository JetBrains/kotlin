import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("A") { test(A()) }
    expectSuccess("A") { test(B()) }
    expectSuccess("C") { test(C()) }
}

private fun test(x: A): String = x.foo()
