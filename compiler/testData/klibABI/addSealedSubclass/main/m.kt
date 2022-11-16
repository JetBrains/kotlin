import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { compute(SC1.C1()) }
    expectSuccess { compute(SC1.O1) }
    expectNoWhenBranchFailure { compute(SC1.Added()) }
    expectSuccess { compute(SC1.C2()) }
    expectSuccess { compute(SC1.O2) }

    expectSuccess { compute(SC2.C1()) }
    expectSuccess { compute(SC2.O1) }
    expectNoWhenBranchFailure { compute(SC2.Added) }
    expectSuccess { compute(SC2.C2()) }
    expectSuccess { compute(SC2.O2) }

    expectSuccess { compute(SI1.C1()) }
    expectSuccess { compute(SI1.O1) }
    expectSuccess { compute(object : SI1.I1 {}) }
    expectNoWhenBranchFailure { compute(SI1.Added()) }
    expectSuccess { compute(SI1.C2()) }
    expectSuccess { compute(SI1.O2) }
    expectSuccess { compute(object : SI1.I2 {}) }

    expectSuccess { compute(SI2.C1()) }
    expectSuccess { compute(SI2.O1) }
    expectSuccess { compute(object : SI2.I1 {}) }
    expectNoWhenBranchFailure { compute(SI2.Added) }
    expectSuccess { compute(SI2.C2()) }
    expectSuccess { compute(SI2.O2) }
    expectSuccess { compute(object : SI2.I2 {}) }

    expectSuccess { compute(SI3.C1()) }
    expectSuccess { compute(SI3.O1) }
    expectSuccess { compute(object : SI3.I1 {}) }
    expectNoWhenBranchFailure { compute(object : SI3.Added {}) }
    expectSuccess { compute(SI3.C2()) }
    expectSuccess { compute(SI3.O2) }
    expectSuccess { compute(object : SI3.I2 {}) }
}
