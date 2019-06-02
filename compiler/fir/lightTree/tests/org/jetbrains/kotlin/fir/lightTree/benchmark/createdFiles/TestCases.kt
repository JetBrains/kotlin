/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.createdFiles

class TestCases {
    /* CLASSES */
    fun `1Class`(): String {
        return TestCasesGenerator().generateData({ createClass(1) }).getText()
    }

    fun `10Classes`(): String {
        return TestCasesGenerator().generateData({ createClass(10) }).getText()
    }

    fun `100Classes`(): String {
        return TestCasesGenerator().generateData({ createClass(100) }).getText()
    }

    fun `1000Classes`(): String {
        return TestCasesGenerator().generateData({ createClass(1000) }).getText()
    }

    fun `10_000Classes`(): String {
        return TestCasesGenerator().generateData({ createClass(10000) }).getText()
    }

    fun `100_000Classes`(): String {
        return TestCasesGenerator().generateData({ createClass(100000) }).getText()
    }

    fun `1Cin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createClass(1)
                })
            }).getText()
    }

    fun `10Cin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createClass(10)
                })
            }).getText()
    }

    fun `100Cin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createClass(100)
                })
            }).getText()
    }

    fun `1000Cin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createClass(1000)
                })
            }).getText()
    }

    fun `10_000Cin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createClass(10_000)
                })
            }).getText()
    }

    fun `100_000Cin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createClass(100_000)
                })
            }).getText()
    }

    fun `1Cin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createClass(1)
                })
            }).getText()
    }

    fun `10Cin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createClass(10)
                })
            }).getText()
    }

    fun `100Cin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createClass(100)
                })
            }).getText()
    }

    fun `1000Cin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createClass(1000)
                })
            }).getText()
    }

    fun `10_000Cin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createClass(10_000)
                })
            }).getText()
    }

    fun `1Cin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createClass(1)
                })
            }).getText()
    }

    fun `10Cin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createClass(10)
                })
            }).getText()
    }

    fun `100Cin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createClass(100)
                })
            }).getText()
    }

    fun `1000Cin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createClass(1000)
                })
            }).getText()
    }

    fun `1Cin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createClass(1)
                })
            }).getText()
    }

    fun `10Cin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createClass(10)
                })
            }).getText()
    }

    fun `100Cin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createClass(100)
                })
            }).getText()
    }

    fun `1Cin10_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10_000, {
                    createClass(1)
                })
            }).getText()
    }

    fun `10Cin10_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10_000, {
                    createClass(10)
                })
            }).getText()
    }

    fun `1Cin100_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100_000, {
                    createClass(1)
                })
            }).getText()
    }

    /* FUNCTIONS */
    fun `1Fun`(): String {
        return TestCasesGenerator().generateData({ createFun(1) }).getText()
    }

    fun `10Fun`(): String {
        return TestCasesGenerator().generateData({ createFun(10) }).getText()
    }

    fun `100Fun`(): String {
        return TestCasesGenerator().generateData({ createFun(100) }).getText()
    }

    fun `1000Fun`(): String {
        return TestCasesGenerator().generateData({ createFun(1000) }).getText()
    }

    fun `10_000Fun`(): String {
        return TestCasesGenerator().generateData({ createFun(10000) }).getText()
    }

    fun `100_000Fun`(): String {
        return TestCasesGenerator().generateData({ createFun(100000) }).getText()
    }

    fun `1Fin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createFun(1)
                })
            }).getText()
    }

    fun `1Fin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createFun(1)
                })
            }).getText()
    }

    fun `1Fin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createFun(1)
                })
            }).getText()
    }

    fun `1Fin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createFun(1)
                })
            }).getText()
    }

    fun `1Fin10_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10_000, {
                    createFun(1)
                })
            }).getText()
    }

    fun `1Fin100_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100_000, {
                    createFun(1)
                })
            }).getText()
    }

    fun `10Fin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createFun(10)
                })
            }).getText()
    }

    fun `10Fin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createFun(10)
                })
            }).getText()
    }

    fun `10Fin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createFun(10)
                })
            }).getText()
    }

    fun `10Fin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createFun(10)
                })
            }).getText()
    }

    fun `10Fin10_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10_000, {
                    createFun(10)
                })
            }).getText()
    }

    fun `100Fin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createFun(100)
                })
            }).getText()
    }

    fun `100Fin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createFun(100)
                })
            }).getText()
    }

    fun `100Fin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createFun(100)
                })
            }).getText()
    }

    fun `100Fin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createFun(100)
                })
            }).getText()
    }

    /* PROPERTIES */
    fun `1Var`(): String {
        return TestCasesGenerator().generateData({ createProperty(1) }).getText()
    }

    fun `10Var`(): String {
        return TestCasesGenerator().generateData({ createProperty(10) }).getText()
    }

    fun `100Var`(): String {
        return TestCasesGenerator().generateData({ createProperty(100) }).getText()
    }

    fun `1000Var`(): String {
        return TestCasesGenerator().generateData({ createProperty(1000) }).getText()
    }

    fun `10_000Var`(): String {
        return TestCasesGenerator().generateData({ createProperty(10000) }).getText()
    }

    fun `100_000Var`(): String {
        return TestCasesGenerator().generateData({ createProperty(100000) }).getText()
    }

    fun `1Vin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createProperty(1)
                })
            }).getText()
    }

    fun `1Vin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createProperty(1)
                })
            }).getText()
    }

    fun `1Vin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createProperty(1)
                })
            }).getText()
    }

    fun `1Vin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createProperty(1)
                })
            }).getText()
    }

    fun `1Vin10_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10_000, {
                    createProperty(1)
                })
            }).getText()
    }

    fun `1Vin100_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100_000, {
                    createProperty(1)
                })
            }).getText()
    }

    fun `10Vin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createProperty(10)
                })
            }).getText()
    }

    fun `10Vin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createProperty(10)
                })
            }).getText()
    }

    fun `10Vin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createProperty(10)
                })
            }).getText()
    }

    fun `10Vin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createProperty(10)
                })
            }).getText()
    }

    fun `10Vin10_000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10_000, {
                    createProperty(10)
                })
            }).getText()
    }

    fun `100Vin1C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1, {
                    createProperty(100)
                })
            }).getText()
    }

    fun `100Vin10C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(10, {
                    createProperty(100)
                })
            }).getText()
    }

    fun `100Vin100C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(100, {
                    createProperty(100)
                })
            }).getText()
    }

    fun `100Vin1000C`(): String {
        return TestCasesGenerator().generateData(
            {
                createClass(1000, {
                    createProperty(100)
                })
            }).getText()
    }
}