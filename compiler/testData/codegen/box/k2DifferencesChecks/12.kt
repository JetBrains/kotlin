// ORIGINAL: /compiler/tests-spec/testData/diagnostics/notLinked/dfa/pos/12.fir.kt
// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 12
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, interfaces, properties, functions
 */

// TESTCASE NUMBER: 1
fun <T> T.case_1() {
    if (this != null) {
        equals(this)
        apply { equals(null) }
        apply { propT }
        apply { propAny }
        apply { propNullableT }
        apply { propNullableAny }
        apply { funT() }
        apply { funAny() }
        apply { funNullableT() }
        apply { funNullableAny(); this.equals(null) }
        also { it.equals(null) }
        also { it.propT }
        also { it.propAny }
        also { it.propNullableT }
        also { it.propNullableAny }
        also { it.funT() }
        also { it.funAny() }
        also { it.funNullableT() }
        also { it.funNullableAny() }
    }
}

// TESTCASE NUMBER: 2
fun <T> T?.case_2() {
    if (this != null) {
        equals(this)
        apply { equals(null) }
        apply { propT }
        apply { propAny }
        apply { propNullableT }
        apply { propNullableAny }
        apply { funT() }
        apply { funAny() }
        apply { funNullableT() }
        apply { funNullableAny(); this.equals(null) }
        also { it.equals(null) }
        also { it.propT }
        also { it.propAny }
        also { it.propNullableT }
        also { it.propNullableAny }
        also { it.funT() }
        also { it.funAny() }
        also { it.funNullableT() }
        also { it.funNullableAny() }
    }
}

// TESTCASE NUMBER: 3
fun <T> T.case_3() {
    if (this != null) {
        this
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
}

// TESTCASE NUMBER: 4
fun <T> T?.case_4() {
    if (this != null) {
        this
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
}

// TESTCASE NUMBER: 5
fun <T> T?.case_5() {
    if (this is Interface1) {
        if (this != null) {
            this
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

            equals(this)
            itest1()
            apply {
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
                itest1()
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
            }
            also {
                it
                it
                it.itest1()
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
fun <T> T?.case_6() {
    if (this is Interface1?) {
        if (this != null) {
            this
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

            equals(this)
            itest1()
            apply {
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
            }
            also {
                it
                it.itest1()
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
fun <T> T.case_7() {
    val x = this
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
            x.itest1()

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
            }
            x.also {
                it
                it.itest1()
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
fun <T> T.case_8() {
    if (this != null) {
        if (this is Interface1?) {
            this
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

            equals(this)
            itest1()
            apply {
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
            }
            also {
                it
                it.itest1()
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
fun <T : Number> T.case_9() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T : Number?> T.case_10() {
    if (this != null) {
        this
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

        equals(this)
        toByte()
        apply {
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
        also {
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
fun <T : Number> T?.case_11() {
    if (this is Interface1?) {
        if (this != null) {
            this
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

            equals(this)
            itest1()
            apply {
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
            }
            also {
                it
                it.itest1()
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
fun <T> T.case_12() where T : Number?, T: Interface1? {
    if (this != null) {
        this
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
        this.toByte()

        equals(this)
        itest1()
        apply {
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
        }
        also {
            it
            it.itest1()
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
fun <T> T.case_13() where T : Out<*>?, T: Comparable<T?> {
    if (this != null) {
        this
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
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: Out<*>?> T.case_14() {
    if (this != null) {
        this
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

        equals(this)
        get()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithFiveTypeParameters1<*, *, *, *, *>?> T.case_15() {
    if (this != null) {
        this
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

        equals(this)
        itest1()
        apply {
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
        }
        also {
            it
            it.itest1()
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> T.case_16() {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> T.case_17() {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<in T>?> T.case_18() {
    val y = this

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

        equals(y)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> T.case_19() {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T.case_20() where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>? {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        ip1test2()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T.case_21() where T: InterfaceWithTypeParameter1<in T>?, T: InterfaceWithTypeParameter2<out T>?, T: InterfaceWithTypeParameter3<T>? {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        ip1test2()
        ip1test3()
        apply {
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
        also {
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
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> T.case_22() {
    var y = this

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

        equals(y)
        ip1test1()
        apply {
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
        also {
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
fun <T: InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<out T>>?> T.case_23() {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<T>?.case_24() {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<T>?.case_25() {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<T>> InterfaceWithTypeParameter1<in T>?.case_26() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<T>> InterfaceWithTypeParameter1<out T>?.case_27() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<out T>?.case_28() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<in T>?.case_29() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<in T>> InterfaceWithTypeParameter1<in T>?.case_30() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<out T>> InterfaceWithTypeParameter1<out T>?.case_31() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> Map<T, *>?.case_32() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> InterfaceWithFiveTypeParameters1<T, *, T, *, T>?.case_33() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> InterfaceWithTypeParameter1<out T>?.case_34() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> InterfaceWithTypeParameter1<in T>?.case_35() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> InterfaceWithTypeParameter1<out T>?.case_36() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> Map<in T, *>?.case_37() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> Map<*, out T>?.case_38() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> InterfaceWithTwoTypeParameters<in T, out T>?.case_39() {
    if (this != null) {
        this
        this.equals(null)
        this.propT
        this.propAny
        this.propNullableT
        this.propNullableAny
        this.funT()
        this.funAny()
        this.funNullableT()
        this.funNullableAny()

        equals(null)

        propT

        propAny

        propNullableT

        propNullableAny

        funT()

        funAny()

        funNullableT()

        funNullableAny()
        apply {
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
        also {
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
fun <T> InterfaceWithTwoTypeParameters<in T, in T>?.case_40() {
    if (this != null) {
        this
        this.equals(null)
        this.propT
        this.propAny
        this.propNullableT
        this.propNullableAny
        this.funT()
        this.funAny()
        this.funNullableT()
        this.funNullableAny()

        equals(null)

        propT

        propAny

        propNullableT

        propNullableAny

        funT()

        funAny()

        funNullableT()

        funNullableAny()
        apply {
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
        also {
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
fun <T> Map<out T, out T>?.case_41() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> Map<T, out T>?.case_42() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> Map<in T, T>?.case_43() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> InterfaceWithFiveTypeParameters1<in T, *, out T, *, T>?.case_44() {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T> T.case_45() where T : Number?, T: Comparable<T>? {
    if (this != null) {
        this
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
        this.compareTo(this)

        equals(this)
        toByte()
        compareTo(this)
        apply {
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
            this.compareTo(this)
            this.toByte()
        }
        also {
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
fun <T> T.case_46() where T : CharSequence?, T: Comparable<T>?, T: Iterable<*>? {
    if (this != null) {
        this
        this.equals(null)
        this.propT
        this.propAny
        this.propNullableT
        this.propNullableAny
        this.funT()
        this.funAny()
        this.funNullableT()
        this.funNullableAny()
        this.compareTo(this)
        this.get(0)
        this.iterator()

        equals(this)
        compareTo(this)
        get(0)
        iterator()
        apply {
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
            this.compareTo(this)
            this.get(0)
            this.iterator()
        }
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_47() where T : Inv<T>, T: Comparable<*>?, T: InterfaceWithTypeParameter1<out T>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }

        this.compareTo(return)
        compareTo(return)

        apply {
            compareTo(return)
            this.compareTo(return)
        }

        also {
            it.compareTo(return)
        }
    }
}

/*
 * TESTCASE NUMBER: 48
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_48() where T : Inv<out T>, T: InterfaceWithTypeParameter1<in T>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 49
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_49() where T : Inv<in T>, T: InterfaceWithTypeParameter1<in T>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 50
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_50() where T : Inv<out T>, T: InterfaceWithTypeParameter1<out T>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 51
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_51() where T : Inv<T>, T: InterfaceWithTypeParameter1<out T>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 52
fun <T> T?.case_52() where T : Inv<in T>, T: InterfaceWithTypeParameter1<T>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 53
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_53() where T : Inv<in T>, T: InterfaceWithTypeParameter1<*>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }
    }
}

/*
 * TESTCASE NUMBER: 54
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T?.case_54() where T : Inv<*>, T: InterfaceWithTypeParameter1<out T?>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 55
fun <T> T?.case_55() where T : Inv<*>, T: InterfaceWithTypeParameter1<T>? {
    if (this != null) {
        this
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
        this.ip1test1()

        equals(this)
        get()
        ip1test1()
        apply {
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
            this.get()
            this.ip1test1()
        }
        also {
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
            it.ip1test1()
        }
    }
}

// TESTCASE NUMBER: 56
fun <T> T.case_56() where T : Number?, T: Interface1? {
    if (this != null) {
        this
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

        equals(this)
        itest()
        toByte()
        apply {
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
            this.itest()
            this.toByte()
        }
        also {
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
fun <T> T.case_57() where T : Out<*>?, T: Comparable<T?> {
    if (this != null) {
        this
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
        apply {
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
        also {
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
fun <T : InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<InterfaceWithTypeParameter1<T>>>>>>>>>>?> T.case_59() {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T.case_59() where T: InterfaceWithFiveTypeParameters1<in T, *, out T?, Nothing?, T>?, T: InterfaceWithFiveTypeParameters2<out T, in T?, T, *, Unit?>?, T: InterfaceWithFiveTypeParameters3<out Nothing, in T, T, in Int?, Number>? {
    if (this != null) {
        this
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

        equals(this)
        itest1()
        itest2()
        itest3()
        apply {
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
        also {
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
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T: InterfaceWithTypeParameter1<out T>?> T.case_60() {
    if (this != null) {
        this
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

        equals(this)
        ip1test1()
        apply {
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
        also {
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

fun <T> T.case_61() where T : InterfaceWithTypeParameter1<T>?, T: Case61_3<T>?, T: Case61_1<T>?, T: Case61_2<T>? {
    if (this != null) {
        this.test1()
        this.test2()
        this.ip1test1()
        this.test4()

        test1()
        test2()
        ip1test1()
        test4()
        apply {
            this
            test1()
            test2()
            ip1test1()
            test4()
            this.test1()
            this.test2()
            this.ip1test1()
            this.test4()
        }
        also {
            it
            it.test1()
            it.test2()
            it.ip1test1()
            it.test4()
        }
    }
}

// TESTCASE NUMBER: 62
fun Nothing?.case_62() {
    if (this != null) {
        this
        this.hashCode()

        hashCode()
        apply {
            this
            hashCode()
            this.hashCode()
        }
        also {
            it
            it.hashCode()
        }
    }
}

// TESTCASE NUMBER: 63
fun Nothing.case_63() {
    if (this != null) {
        this
        this.hashCode()

        hashCode()
        apply {
            this
            hashCode()
            this.hashCode()
        }
        also {
            it
            it.hashCode()
        }
    }
}

/*
 * TESTCASE NUMBER: 64
 * UNEXPECTED BEHAVIOUR
 */
fun <T : Nothing?> T.case_64() {
    if (this != null) {
        this
        this.hashCode()

        hashCode()
        apply {
        this
        hashCode()
        this.hashCode()
    }
        also {
        it
        it.hashCode()
    }
    }
}

/*
 * TESTCASE NUMBER: 65
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28785
 */
fun <T> T.case_65() {
    if (this is Interface1?) {
        if (this is Interface2?) {
            if (this != null) {
                this
                this.equals(null)
                this.propT
                this.propAny
                this.propNullableT
                this.propNullableAny
                this.funT()
                this.funAny()
                this.funNullableT()
                this.funNullableAny()
                apply {
                    this
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
                also {
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
    }
}


fun box() = "OK"
