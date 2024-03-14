// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT
// WITH_EXTENDED_CHECKERS

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 1
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, functions, typealiases, properties, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x != null is Boolean) {
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

// TESTCASE NUMBER: 3
fun case_3() {
    if (Object.prop_1 == null !== null)
    else {
        Object.prop_1
        Object.prop_1.equals(null)
        Object.prop_1.propT
        Object.prop_1.propAny
        Object.prop_1.propNullableT
        Object.prop_1.propNullableAny
        Object.prop_1.funT()
        Object.prop_1.funAny()
        Object.prop_1.funNullableT()
        Object.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char?) {
    if (x != null || false is Boolean) {
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
fun case_5() {
    val x: Unit? = null

    if (x !== null is Boolean?) x
    if (x !== null == null) x.equals(null)
    if (x !== null == null) x.propT
    if (x !== null == null) x.propAny
    if (x !== null == null) x.propNullableT
    if (x !== null == null) x.propNullableAny
    if (x !== null == null) x.funT()
    if (x !== null == null) x.funAny()
    if (x !== null == null) x.funNullableT()
    if (x !== null == null) x.funNullableAny()
}

// TESTCASE NUMBER: 6
fun case_6(x: EmptyClass?) {
    val y = true

    if ((x != null && !y) is Boolean) {
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

// TESTCASE NUMBER: 7
fun case_7() {
    if (nullableNumberProperty != null || nullableNumberProperty != null is Boolean) {
        nullableNumberProperty
        nullableNumberProperty.equals(null)
        nullableNumberProperty.propT
        nullableNumberProperty.propAny
        nullableNumberProperty.propNullableT
        nullableNumberProperty.propNullableAny
        nullableNumberProperty.funT()
        nullableNumberProperty.funAny()
        nullableNumberProperty.funNullableT()
        nullableNumberProperty.funNullableAny()
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: TypealiasNullableString) {
    if (x !== null === null && x != null != null) x.get(0)
    if (x !== null != null && x != null === null) x.get(0)
}

// TESTCASE NUMBER: 9
fun case_9(x: TypealiasNullableString?) {
    if (x === null === null) {

    } else if (false is Boolean) {
        x
        x.get(0)
    }
}

// TESTCASE NUMBER: 10
fun case_10() {
    val a = Class()

    if (a.prop_4 === null || true is Boolean) {
        if (a.prop_4 != null !== null) {
            a.prop_4
            a.prop_4.equals(null)
            a.prop_4.propT
            a.prop_4.propAny
            a.prop_4.propNullableT
            a.prop_4.propNullableAny
            a.prop_4.funT()
            a.prop_4.funAny()
            a.prop_4.funNullableT()
            a.prop_4.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: TypealiasNullableStringIndirect?, y: TypealiasNullableStringIndirect) {
    val t: TypealiasNullableStringIndirect = null

    if (x == null is Boolean) {

    } else {
        if (y != null is Boolean == true) {
            if ((nullableStringProperty == null) !is Boolean) {
                if (t != null is Boolean) {
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
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: TypealiasNullableStringIndirect, y: TypealiasNullableStringIndirect) =
    if ((x == null) !is Boolean === false) "1"
    else if ((y === null !== null) is Boolean) x
    else if (y === null != null) x.equals(null)
    else if (y === null != null) x.propT
    else if (y === null != null) x.propAny
    else if (y === null != null) x.propNullableT
    else if (y === null != null) x.propNullableAny
    else if (y === null != null) x.funT()
    else if (y === null != null) x.funAny()
    else if (y === null != null) x.funNullableT()
    else if (y === null != null) x.funNullableAny()
    else "-1"

// TESTCASE NUMBER: 13
fun case_13(x: otherpackage.Case13?) =
    if ((x == null !is Boolean) !== true) {
        throw Exception()
    } else {
        x
        x.equals(x)
    }

// TESTCASE NUMBER: 14
class Case14 {
    val x: otherpackage.Case14?
    init {
        x = otherpackage.Case14()
    }
}

@Suppress("UNREACHABLE_CODE")
fun case_14() {
    val a = Case14()

    if (a.x != null !is Boolean !is Boolean) {
        if (a.x != null == true) {
            if (a.x !== null == false) {
                if (a.x != null == null) {
                    if (a.x != null !== null) {
                        if (a.x != null === true) {
                            if (a.x !== null === true !is Boolean == true) {
                                if (a.x != null !== false) {
                                    if (a.x != null === false) {
                                        if (a.x !== null === true) {
                                            if ((a.x != null != true) !is Boolean) {
                                                if (a.x != null is Boolean) {
                                                    if (a.x != null is Boolean is Boolean) {
                                                        if (a.x !== null is Boolean) {
                                                            if (a.x != null is Boolean) {
                                                                if ((a.x !== null !is Boolean) == false) {
                                                                    a.x
                                                                    a.x.equals(a.x)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: EmptyObject) {
    val t = if (x === null is Boolean is Boolean is Boolean) "" else {
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

// TESTCASE NUMBER: 16
fun case_16() {
    val x: TypealiasNullableNothing = null

    if (x != null !is Boolean !is Boolean !is Boolean !is Boolean !is Boolean) {
        x
        x.java
    }
}

// TESTCASE NUMBER: 17
val case_17 = if (nullableIntProperty == null == true == false) 0 else {
    nullableIntProperty
    nullableIntProperty.java
}

//TESTCASE NUMBER: 18
fun case_18(a: DeepObject.A.B.C.D.E.F.G.J?) {
    if (a != null !== null) {
        a
        a.equals(null)
        a.propT
        a.propAny
        a.propNullableT
        a.propNullableAny
        a.funT()
        a.funAny()
        a.funNullableT()
        a.funNullableAny()
    }
}

// TESTCASE NUMBER: 19
fun case_19(b: Boolean) {
    val a = if (b) {
        object {
            val B19 = if (b) {
                object {
                    val C19 = if (b) {
                        object {
                            val D19 = if (b) {
                                object {
                                    val x: Number? = 10
                                }
                            } else null
                        }
                    } else null
                }
            } else null
        }
    } else null

    if (a != null !is Boolean && a.B19 != null is Boolean && a.B19.C19 != null is Boolean && a.B19.C19.D19 != null == null && a.B19.C19.D19.x != null !== null) {
        a.B19.C19.D19.x
        a.B19.C19.D19.x.equals(null)
        a.B19.C19.D19.x.propT
        a.B19.C19.D19.x.propAny
        a.B19.C19.D19.x.propNullableT
        a.B19.C19.D19.x.propNullableAny
        a.B19.C19.D19.x.funT()
        a.B19.C19.D19.x.funAny()
        a.B19.C19.D19.x.funNullableT()
        a.B19.C19.D19.x.funNullableAny()
    }
}

// TESTCASE NUMBER: 20
fun case_20(b: Boolean) {
    val a = object {
        val B19 = object {
            val C19 = object {
                val D19 =  if (b) {
                    object {}
                } else null
            }
        }
    }

    if (a.B19.C19.D19 !== null !is Boolean) {
        a.B19.C19.D19
        a.B19.C19.D19.equals(null)
        a.B19.C19.D19.propT
        a.B19.C19.D19.propAny
        a.B19.C19.D19.propNullableT
        a.B19.C19.D19.propNullableAny
        a.B19.C19.D19.funT()
        a.B19.C19.D19.funAny()
        a.B19.C19.D19.funNullableT()
        a.B19.C19.D19.funNullableAny()
    }
}

// TESTCASE NUMBER: 21
fun case_21() {
    if (EnumClassWithNullableProperty.B.prop_1 !== null is Boolean == true !is Boolean != true) {
        EnumClassWithNullableProperty.B.prop_1
        EnumClassWithNullableProperty.B.prop_1.equals(null)
        EnumClassWithNullableProperty.B.prop_1.propT
        EnumClassWithNullableProperty.B.prop_1.propAny
        EnumClassWithNullableProperty.B.prop_1.propNullableT
        EnumClassWithNullableProperty.B.prop_1.propNullableAny
        EnumClassWithNullableProperty.B.prop_1.funT()
        EnumClassWithNullableProperty.B.prop_1.funAny()
        EnumClassWithNullableProperty.B.prop_1.funNullableT()
        EnumClassWithNullableProperty.B.prop_1.funNullableAny()
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)?) {
    if (a != null !is Boolean) {
        a()
        a().equals(null)
        a().propT
        a().propAny
        a().propNullableT
        a().propNullableAny
        a().funT()
        a().funAny()
        a().funNullableT()
        a().funNullableAny()
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null !is Boolean && b !== null is Boolean) {
        val x = a(b)
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
}

// TESTCASE NUMBER: 24
fun case_24(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?) =
    if (a !== null is Boolean && b !== null !is Boolean) {
        a(b)
        a(b)
        b.equals(null)
        b.propT
        b.propAny
        b.propNullableT
        b.propNullableAny
        b.funT()
        b.funAny()
        b.funNullableT()
        b.funNullableAny()
    } else null

// TESTCASE NUMBER: 25
fun case_25(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else null
    }

    val y = if (b) x else null

    if (y !== null === true) {
        val z = y()

        if (z != null !== false) {
            z.a
            z.a.equals(null)
            z.a.propT
            z.a.propAny
            z.a.propNullableT
            z.a.propNullableAny
            z.a.funT()
            z.a.funAny()
            z.a.funNullableT()
            z.a.funNullableAny()
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null == true == false && b != null == true == false) {
        val x = a(b)
        if (x != null == true === false) {
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
}

// TESTCASE NUMBER: 27
fun case_27() {
    if (Object.prop_1 == null == true == true == true == true == true == true == true == true == true == true == true == true == true == true is Boolean)
    else {
        Object.prop_1
        Object.prop_1.equals(null)
        Object.prop_1.propT
        Object.prop_1.propAny
        Object.prop_1.propNullableT
        Object.prop_1.propNullableAny
        Object.prop_1.funT()
        Object.prop_1.funAny()
        Object.prop_1.funNullableT()
        Object.prop_1.funNullableAny()
    }
}
