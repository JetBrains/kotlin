import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess {
        C()
        "OK"
    }
}