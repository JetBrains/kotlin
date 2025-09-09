import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("fooFromLib1.v2") { fooFromLib1() }
    expectSuccess("fooFromLib1.v1") { fooFromLib2() }
}
