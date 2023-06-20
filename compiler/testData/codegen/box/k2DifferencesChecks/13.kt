// ORIGINAL: /compiler/tests-spec/testData/diagnostics/notLinked/dfa/pos/13.fir.kt
// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 13
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, interfaces, properties, functions
 */

// TESTCASE NUMBER: 1
fun <T> case_1(x: T) {
    var y = null

    if (y != x) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.apply { equals(null) }
        x.apply { propT }
        x.apply { propAny }
        x.apply { propNullableT }
        x.apply { propNullableAny }
        x.apply { funT() }
        x.apply { funAny() }
        x.apply { funNullableT() }
        x.apply { funNullableAny(); x.equals(null) }
        x.also { it.equals(null) }
        x.also { it.propT }
        x.also { it.propAny }
        x.also { it.propNullableT }
        x.also { it.propNullableAny }
        x.also { it.funT() }
        x.also { it.funAny() }
        x.also { it.funNullableT() }
        x.also { it.funNullableAny() }
    }
}

// TESTCASE NUMBER: 2
fun <T> case_2(x: T?, y: Nothing?) {
    if (y != x) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.apply { equals(null) }
        x.apply { propT }
        x.apply { propAny }
        x.apply { propNullableT }
        x.apply { propNullableAny }
        x.apply { funT() }
        x.apply { funAny() }
        x.apply { funNullableT() }
        x.apply { funNullableAny(); x.equals(null) }
        x.also { it.equals(null) }
        x.also { it.propT }
        x.also { it.propAny }
        x.also { it.propNullableT }
        x.also { it.propNullableAny }
        x.also { it.funT() }
        x.also { it.funAny() }
        x.also { it.funNullableT() }
        x.also { it.funNullableAny() }
    }
}

// TESTCASE NUMBER: 3
fun <T> case_3(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun <T> case_4(x: T?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
    }
}

// TESTCASE NUMBER: 5
fun <T> case_5(x: T?) {
    if (x is Interface1) {
        if (x != null) {
            x
            x.equals(null)
            x.propT
            x.propAny
            x.propNullableT
            x.propNullableAny
            x.funT()
            x.funAny()
            x.funNullableT()
            x.funNullableAny()
            x.itest()

            x.equals(null)

            x.propT

            x.propAny

            x.propNullableT

            x.propNullableAny

            x.funT()

            x.funAny()

            x.funNullableT()

            x.funNullableAny()
            x.itest()
            x.apply {
                this
                this
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                this.equals(null)
                this.propT
                this.propAny
                this.propNullableT
                this.propNullableAny
                this.funT()
                this.funAny()
                this.funNullableT()
                this.funNullableAny()
                this.itest()
            }
            x.also {
                it
                it
                it.itest()
                it.equals(null)
                it.propT
                it.propAny
                it.propNullableT
                it.propNullableAny
                it.funT()
                it.funAny()
                it.funNullableT()
                it.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 6
fun <T> case_6(x: T?) {
    if (x is Interface1?) {
        if (x != null) {
            x
            x.equals(null)
            x.propT
            x.propAny
            x.propNullableT
            x.propNullableAny
            x.funT()
            x.funAny()
            x.funNullableT()
            x.funNullableAny()
            x.itest()

            x.equals(null)

            x.propT

            x.propAny

            x.propNullableT

            x.propNullableAny

            x.funT()

            x.funAny()

            x.funNullableT()

            x.funNullableAny()
            x.itest()
            x.apply {
                this
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                this.equals(null)
                this.propT
                this.propAny
                this.propNullableT
                this.propNullableAny
                this.funT()
                this.funAny()
                this.funNullableT()
                this.funNullableAny()
                this.itest()
            }
            x.also {
                it
                it.itest()
                it.equals(null)
                it.propT
                it.propAny
                it.propNullableT
                it.propNullableAny
                it.funT()
                it.funAny()
                it.funNullableT()
                it.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 7
fun <T> case_7(y: T) {
    val x = y
    if (x is Interface1?) {
        if (x != null) {
            x
            x.equals(null)
            x.propT
            x.propAny
            x.propNullableT
            x.propNullableAny
            x.funT()
            x.funAny()
            x.funNullableT()
            x.funNullableAny()
            x.itest()

            x.apply {
                this
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                this.equals(null)
                this.propT
                this.propAny
                this.propNullableT
                this.propNullableAny
                this.funT()
                this.funAny()
                this.funNullableT()
                this.funNullableAny()
                this.itest()
            }
            x.also {
                it
                it.itest()
                it.equals(null)
                it.propT
                it.propAny
                it.propNullableT
                it.propNullableAny
                it.funT()
                it.funAny()
                it.funNullableT()
                it.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 8
fun <T> case_8(x: T) {
    if (x != null) {
        if (x is Interface1?) {
            x
            x.equals(null)
            x.propT
            x.propAny
            x.propNullableT
            x.propNullableAny
            x.funT()
            x.funAny()
            x.funNullableT()
            x.funNullableAny()
            x.itest()

            x.equals(null)

            x.propT

            x.propAny

            x.propNullableT

            x.propNullableAny

            x.funT()

            x.funAny()

            x.funNullableT()

            x.funNullableAny()
            x.itest()
            x.apply {
                this
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                this.equals(null)
                this.propT
                this.propAny
                this.propNullableT
                this.propNullableAny
                this.funT()
                this.funAny()
                this.funNullableT()
                this.funNullableAny()
                this.itest()
            }
            x.also {
                it
                it.itest()
                it.equals(null)
                it.propT
                it.propAny
                it.propNullableT
                it.propNullableAny
                it.funT()
                it.funAny()
                it.funNullableT()
                it.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 9
fun <T : Number> case_9(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.toByte()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.toByte()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            x.toByte()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.toByte()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.toByte()
        }
    }
}

// TESTCASE NUMBER: 10
fun <T : Number?> case_10(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.toByte()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.toByte()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            toByte()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.toByte()
        }
        x.also {
            it
            it.toByte()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 11
fun <T : Number> case_11(x: T?) {
    if (x is Interface1?) {
        if (x != null) {
            x
            x.equals(null)
            x.propT
            x.propAny
            x.propNullableT
            x.propNullableAny
            x.funT()
            x.funAny()
            x.funNullableT()
            x.funNullableAny()
            x.itest()

            x.equals(null)

            x.propT

            x.propAny

            x.propNullableT

            x.propNullableAny

            x.funT()

            x.funAny()

            x.funNullableT()

            x.funNullableAny()
            x.itest()
            x.apply {
                this
                equals(null)
                propT
                propAny
                propNullableT
                propNullableAny
                funT()
                funAny()
                funNullableT()
                funNullableAny()
                itest()
                this.equals(null)
                this.propT
                this.propAny
                this.propNullableT
                this.propNullableAny
                this.funT()
                this.funAny()
                this.funNullableT()
                this.funNullableAny()
                this.itest()
            }
            x.also {
                it
                it.itest()
                it.equals(null)
                it.propT
                it.propAny
                it.propNullableT
                it.propNullableAny
                it.funT()
                it.funAny()
                it.funNullableT()
                it.funNullableAny()
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun <T> case_12(x: T) where T : Number?, T: Interface1? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.itest()
        x.toByte()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.itest()
        }
        x.also {
            it
            it.itest()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_13(x: T) where T : Out<*>?, T: Comparable<T?> {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.get()
        x.compareTo(null)

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.get()
        x.compareTo(null)
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            get()
            compareTo(null)
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.get()
            this.compareTo(null)
        }
        x.also {
            it
            it.get()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.compareTo(null)
        }
    }
}

/*
 * TESTCASE NUMBER: 14
 * ISSUES: KT-28785
 */
fun <T: Out<*>?> case_14(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.get()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.get()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            get()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.get()
        }
        x.also {
            it
            it.get()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 15
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithFiveTypeParameters1<*, *, *, *, *>?> case_15(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.itest()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.itest()
        }
        x.also {
            it
            it.itest()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 16
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_16(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.ip1test1()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 17
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> case_17(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.ip1test1()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 18
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> case_18(x: T) {
    val y = x

    if (y != null) {
        y
        y.equals(null)
        y.propT
        y.propAny
        y.propNullableT
        y.propNullableAny
        y.funT()
        y.funAny()
        y.funNullableT()
        y.funNullableAny()
        y.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.ip1test1()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 19
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_19(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.ip1test1()
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

/*
 * TESTCASE NUMBER: 20
 * ISSUES: KT-28785
 */
fun <T> case_20(x: T) where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()
        x.ip1test2()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.ip1test2()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            ip1test2()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
            this.ip1test2()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
            it.ip1test2()
        }
    }
}

/*
 * TESTCASE NUMBER: 21
 * ISSUES: KT-28785
 */
fun <T> case_21(x: T) where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>?, T: InterfaceWithTypeParameter3<T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()
        x.ip1test2()
        x.ip1test3()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.ip1test2()
        x.ip1test3()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            ip1test2()
            ip1test3()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
            this.ip1test2()
            this.ip1test3()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
            it.ip1test2()
            it.ip1test3()
        }
    }
}

/*
 * TESTCASE NUMBER: 22
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> case_22(x: T) {
    var y = x

    if (y != null) {
        y
        y.equals(null)
        y.propT
        y.propAny
        y.propNullableT
        y.propNullableAny
        y.funT()
        y.funAny()
        y.funNullableT()
        y.funNullableAny()
        y.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(this)
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 23
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> case_23(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 24
fun <T : InterfaceWithTypeParameter1<in T>> case_24(x: InterfaceWithTypeParameter1<T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 25
fun <T : InterfaceWithTypeParameter1<out T>> case_25(x: InterfaceWithTypeParameter1<T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 26
fun <T : InterfaceWithTypeParameter1<T>> case_26(x: InterfaceWithTypeParameter1<in T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 27
fun <T : InterfaceWithTypeParameter1<T>> case_27(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 28
fun <T : InterfaceWithTypeParameter1<in T>> case_28(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 29
fun <T : InterfaceWithTypeParameter1<out T>> case_29(x: InterfaceWithTypeParameter1<in T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 30
fun <T : InterfaceWithTypeParameter1<in T>> case_30(x: InterfaceWithTypeParameter1<in T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 31
fun <T : InterfaceWithTypeParameter1<out T>> case_31(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 32
fun <T> case_32(x: Map<T, *>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.isEmpty()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 33
fun <T> case_33(x: InterfaceWithFiveTypeParameters1<T, *, T, *, T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.itest()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.itest()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.itest()
        }
    }
}

// TESTCASE NUMBER: 34
fun <T> case_34(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 35
fun <T> case_35(x: InterfaceWithTypeParameter1<in T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 36
fun <T> case_36(x: InterfaceWithTypeParameter1<out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 37
fun <T> case_37(x: Map<in T, *>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.isEmpty()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 38
fun <T> case_38(x: Map<*, out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.isEmpty()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 39
fun <T> case_39(x: InterfaceWithTwoTypeParameters<in T, out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 40
fun <T> case_40(x: InterfaceWithTwoTypeParameters<in T, in T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 41
fun <T> case_41(x: Map<out T, out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.isEmpty()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 42
fun <T> case_42(x: Map<T, out T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.isEmpty()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 43
fun <T> case_43(x: Map<in T, T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.isEmpty()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.isEmpty()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            isEmpty()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.isEmpty()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.isEmpty()
        }
    }
}

// TESTCASE NUMBER: 44
fun <T> case_44(x: InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.itest()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.itest()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.itest()
        }
    }
}

// TESTCASE NUMBER: 45
fun <T> case_45(x: T) where T : Number?, T: Comparable<T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.toByte()
        x.compareTo(x)

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.toByte()
        x.compareTo(x)
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            compareTo(this)
            x.toByte()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.compareTo(x)
            this.toByte()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.compareTo(it)
            it.toByte()
        }
    }
}

// TESTCASE NUMBER: 46
fun <T> case_46(x: T) where T : CharSequence?, T: Comparable<T>?, T: Iterable<*>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.compareTo(x)
        x.get(0)
        x.iterator()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.compareTo(x)
        x.get(0)
        x.iterator()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            compareTo(this)
            get(0)
            iterator()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.compareTo(x)
            this.get(0)
            this.iterator()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.compareTo(it)
            it.get(0)
            it.iterator()
        }
    }
}

/*
 * TESTCASE NUMBER: 47
 * ISSUES: KT-28785
 */
fun <T> case_47(x: T?) where T : Inv<T>, T: Comparable<*>?, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }

        x.compareTo(return)
        x.compareTo(return)

        x.apply {
            compareTo(return)
            this.compareTo(return)
        }

        x.also {
            it.compareTo(return)
        }
    }
}

/*
 * TESTCASE NUMBER: 48
 * ISSUES: KT-28785
 */
fun <T> case_48(x: T?) where T : Inv<out T>, T: InterfaceWithTypeParameter1<in T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 49
 * ISSUES: KT-28785
 */
fun <T> case_49(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<in T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 50
 * ISSUES: KT-28785
 */
fun <T> case_50(x: T?) where T : Inv<out T>, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 51
 * ISSUES: KT-28785
 */
fun <T> case_51(x: T?) where T : Inv<T>, T: InterfaceWithTypeParameter1<out T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 52
fun <T> case_52(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 53
 * ISSUES: KT-28785
 */
fun <T> case_53(x: T?) where T : Inv<in T>, T: InterfaceWithTypeParameter1<*>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 54
 * ISSUES: KT-28785
 */
fun <T> case_54(x: T?) where T : Inv<*>, T: InterfaceWithTypeParameter1<out T?>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 55
fun <T> case_55(x: T?) where T : Inv<*>, T: InterfaceWithTypeParameter1<T>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.test()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.test()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            test()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.test()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.test()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 56
fun <T> case_56(x: T) where T : Number?, T: Interface1? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.itest()
        x.toByte()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest()
        x.toByte()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest()
            x.toByte()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.itest()
            this.toByte()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.itest()
            it.toByte()
        }
    }
}

/*
 * TESTCASE NUMBER: 57
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> case_57(x: T) where T : Out<*>?, T: Comparable<T?> {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.get()
        x.compareTo(null)

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.get()
        x.compareTo(null)
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            get()
            compareTo(null)
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.get()
            this.compareTo(null)
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.get()
            it.compareTo(null)
        }
    }
}

// TESTCASE NUMBER: 58
fun <T : InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<T>>>>>>>>>>?> case_59(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 59
 * ISSUES: KT-28785
 */
fun <T> case_59(x: T) where T: InterfaceWithFiveTypeParameters1<in T, *, out T?, Nothing?, T>?, T: InterfaceWithFiveTypeParameters2<out T, in T?, T, *, Unit?>?, T: InterfaceWithFiveTypeParameters3<out Nothing, in T, T, in Int?, Number>? {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.itest1()
        x.itest2()
        x.itest3()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.itest1()
        x.itest2()
        x.itest3()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            itest1()
            itest2()
            itest3()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.itest1()
            this.itest2()
            this.itest3()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.itest1()
            it.itest2()
            it.itest3()
        }
    }
}

/*
 * TESTCASE NUMBER: 60
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> case_60(x: T) {
    if (x != null) {
        x
        x.equals(null)
        x.propT
        x.propAny
        x.propNullableT
        x.propNullableAny
        x.funT()
        x.funAny()
        x.funNullableT()
        x.funNullableAny()
        x.ip1test1()

        x.equals(null)

        x.propT

        x.propAny

        x.propNullableT

        x.propNullableAny

        x.funT()

        x.funAny()

        x.funNullableT()

        x.funNullableAny()
        x.ip1test1()
        x.apply {
            this
            equals(null)
            propT
            propAny
            propNullableT
            propNullableAny
            funT()
            funAny()
            funNullableT()
            funNullableAny()
            ip1test1()
            this.equals(null)
            this.propT
            this.propAny
            this.propNullableT
            this.propNullableAny
            this.funT()
            this.funAny()
            this.funNullableT()
            this.funNullableAny()
            this.ip1test1()
        }
        x.also {
            it
            it.equals(null)
            it.propT
            it.propAny
            it.propNullableT
            it.propNullableAny
            it.funT()
            it.funAny()
            it.funNullableT()
            it.funNullableAny()
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 61
interface Case61_1<T>: InterfaceWithTypeParameter1<T>, Case61_2<T> { fun test1() }
interface Case61_2<T>: InterfaceWithTypeParameter1<T> { fun test2() }

class Case61_3<T>: InterfaceWithTypeParameter1<T>, Case61_1<T>, Case61_2<T> {
    override fun test1() {}
    override fun test2() {}
    fun test4() {}
}

fun <T> T.case_61(x: T) where T : InterfaceWithTypeParameter1<T>?, T: Case61_3<T>?, T: Case61_1<T>?, T: Case61_2<T>? {
    if (x != null) {
        x.ip1test1()
        x.test2()
        x.ip1test1()
        x.test4()

        x.ip1test1()
        x.test2()
        x.ip1test1()
        x.test4()
        x.apply {
            this
            ip1test1()
            test2()
            ip1test1()
            test4()
            this.ip1test1()
            this.test2()
            this.ip1test1()
            this.test4()
        }
        x.also {
            it
            it.ip1test1()
            it.test2()
            it.ip1test1()
            it.test4()
        }
    }
}

/*
 * TESTCASE NUMBER: 62
 * UNEXPECTED BEHAVIOUR
 */
fun <T : Nothing?> case_62(x: T) {
    if (x != null) {
        x
        x.hashCode()

        x.hashCode()
        x.apply {
            this
            hashCode()
            this.hashCode()
        }
        x.also {
            it
            it.hashCode()
        }
    }
}


fun box() = "OK"
