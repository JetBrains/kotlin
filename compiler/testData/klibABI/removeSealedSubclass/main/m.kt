import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Expression can not be evaluated: IR expression uses unlinked class symbol /SC1.Removed")) { compute(SC1.O2) }
    expectFailure(linkage("Expression can not be evaluated: IR expression uses unlinked class symbol /SC1.Removed")) { compute(SC1.C2()) }
    expectFailure(linkage("Constructor Removed.<init> can not be called: No constructor found for symbol /SC1.Removed.<init>")) { compute(SC1.Removed()) }
    expectSuccess { compute(SC1.O1) }
    expectSuccess { compute(SC1.C1()) }

    expectFailure(linkage("Can not get instance of singleton Removed: No class found for symbol /SC2.Removed")) { compute(SC2.O2) }
    expectFailure(linkage("Can not get instance of singleton Removed: No class found for symbol /SC2.Removed")) { compute(SC2.C2()) }
    expectFailure(linkage("Can not get instance of singleton Removed: No class found for symbol /SC2.Removed")) { compute(SC2.Removed) }
    expectSuccess { compute(SC2.O1) }
    expectSuccess { compute(SC2.C1()) }

    expectFailure(linkage("Expression can not be evaluated: IR expression uses unlinked class symbol /SI1.Removed")) { compute(object : SI1.I2 {}) }
    expectFailure(linkage("Expression can not be evaluated: IR expression uses unlinked class symbol /SI1.Removed")) { compute(SI1.O2) }
    expectFailure(linkage("Expression can not be evaluated: IR expression uses unlinked class symbol /SI1.Removed")) { compute(SI1.C2()) }
    expectFailure(linkage("Constructor Removed.<init> can not be called: No constructor found for symbol /SI1.Removed.<init>")) { compute(SI1.Removed()) }
    expectSuccess { compute(object : SI1.I1 {}) }
    expectSuccess { compute(SI1.O1) }
    expectSuccess { compute(SI1.C1()) }

    expectFailure(linkage("Can not get instance of singleton Removed: No class found for symbol /SI2.Removed")) { compute(object : SI2.I2 {}) }
    expectFailure(linkage("Can not get instance of singleton Removed: No class found for symbol /SI2.Removed")) { compute(SI2.O2) }
    expectFailure(linkage("Can not get instance of singleton Removed: No class found for symbol /SI2.Removed")) { compute(SI2.C2()) }
    expectFailure(linkage("Can not get instance of singleton Removed: No class found for symbol /SI2.Removed")) { compute(SI2.Removed) }
    expectSuccess { compute(object : SI2.I1 {}) }
    expectSuccess { compute(SI2.O1) }
    expectSuccess { compute(SI2.C1()) }

    expectFailure(linkage("Expression can not be evaluated: IR expression uses unlinked class symbol /SI3.Removed")) { compute(object : SI3.I2 {}) }
    expectFailure(linkage("Expression can not be evaluated: IR expression uses unlinked class symbol /SI3.Removed")) { compute(SI3.O2) }
    expectFailure(linkage("Expression can not be evaluated: IR expression uses unlinked class symbol /SI3.Removed")) { compute(SI3.C2()) }
    expectFailure(linkage("Constructor <init> can not be called: Constructor <init> uses unlinked class symbol /SI3.Removed (through anonymous object)")) { compute(object : SI3.Removed {}) }
    expectSuccess { compute(object : SI3.I1 {}) }
    expectSuccess { compute(SI3.O1) }
    expectSuccess { compute(SI3.C1()) }
}
