import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { C2().foo + I2().foo }
}
