import abitestutils.abiTest

fun box() = abiTest {
    class SI1_C : SI1
    class SC1_C : SC1()
    class E1_C : E1()
    class E2_C : E2()

    expectSuccess("A") { computeSI1(SI1.A()) }
    expectFailure(noWhenBranch()) { computeSI1(SI1_C()) }
    expectSuccess("B") { computeSI1(SI1.B()) }

    expectSuccess("A") { computeSC1(SC1.A()) }
    expectFailure(noWhenBranch()) { computeSC1(SC1_C()) }
    expectSuccess("B") { computeSC1(SC1.B()) }

    expectFailure(linkage("Can not get instance of singleton 'E1.A': No enum entry found for symbol '/E1.A'")) { computeE1(E1.A) }
    expectFailure(linkage("Can not get instance of singleton 'E1.A': No enum entry found for symbol '/E1.A'")) { computeE1(E1_C()) }
    expectFailure(linkage("Can not get instance of singleton 'E1.A': No enum entry found for symbol '/E1.A'")) { computeE1(E1.B) }

    expectFailure(linkage("Can not get instance of singleton 'E2.A': No enum entry found for symbol '/E2.A'")) { computeE2(E2.A()) }
    expectFailure(linkage("Can not get instance of singleton 'E2.A': No enum entry found for symbol '/E2.A'")) { computeE2(E2_C()) }
    expectFailure(linkage("Can not get instance of singleton 'E2.A': No enum entry found for symbol '/E2.A'")) { computeE2(E2.B()) }

    expectSuccess("ClassToObject") { computeSI2(SI2.ClassToObject) }
    expectFailure(linkage("Can not get instance of singleton 'ObjectToClass': 'ObjectToClass' is class while object is expected")) { computeSI2(SI2.ObjectToClass()) }

    expectSuccess("ClassToObject") { computeSC2(SC2.ClassToObject) }
    expectFailure(linkage("Can not get instance of singleton 'ObjectToClass': 'ObjectToClass' is class while object is expected")) { computeSC2(SC2.ObjectToClass()) }
}
