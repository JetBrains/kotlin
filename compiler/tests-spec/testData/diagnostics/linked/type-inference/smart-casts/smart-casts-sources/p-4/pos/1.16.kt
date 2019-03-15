// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION, -NAME_SHADOWING, -UNUSED_VARIABLE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 4 -> sentence 1
 * RELEVANT PLACES:
 *      paragraph 1 -> sentence 2
 *      paragraph 6 -> sentence 1
 *      paragraph 9 -> sentence 3
 *      paragraph 9 -> sentence 4
 * NUMBER: 16
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality with `null` and `return` after it) using if expression.
 * UNSPECIFIED BEHAVIOR
 * HELPERS: objects, properties, classes
 */

// TESTCASE NUMBER: 1
fun case_1(x: Int?) {
    if (x == null) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

// TESTCASE NUMBER: 2
fun case_2(x: Unit?) {
    if (x === null) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 3
fun case_3(x: Nothing?) {
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) else return
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.equals(<!DEBUG_INFO_CONSTANT!>x<!>)
}

// TESTCASE NUMBER: 4
fun case_4(x: Number?) {
    if (x !== null) else { return }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 5
fun case_5(x: Char?, y: Nothing?) {
    if (x != <!DEBUG_INFO_CONSTANT!>y<!>) else return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 6
fun case_6(x: Object?) {
    if (x !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) else { return }
    <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 7
fun case_7(x: Class?) {
    if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || false || false || false) { return }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 8
fun case_8(x: Int?) {
    if (false || false || false || x == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

// TESTCASE NUMBER: 9
fun case_9(x: String?) {
    if (x != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && true && true && true) else { return }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 10
fun case_10(x: Float?) {
    if (true && true && true && x !== null) else return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 11
fun case_11(x: Out<*>?) {
    if (x == null) return
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 12
fun case_12(x: Map<Unit, Nothing?>?) {
    if (x === null) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<kotlin.Unit, kotlin.Nothing?> & kotlin.collections.Map<kotlin.Unit, kotlin.Nothing?>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<kotlin.Unit, kotlin.Nothing?> & kotlin.collections.Map<kotlin.Unit, kotlin.Nothing?>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 13
fun case_13(x: Map<out Number, *>?) {
    if (x != null) else return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 14
fun case_14(x: MutableCollection<in Number>?) {
    if (x !== null) else { return }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 15
fun case_15(x: MutableCollection<out Nothing?>?, y: Nothing?) {
    if (x != <!DEBUG_INFO_CONSTANT!>y<!>) else return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<out kotlin.Nothing?> & kotlin.collections.MutableCollection<out kotlin.Nothing?>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<out kotlin.Nothing?> & kotlin.collections.MutableCollection<out kotlin.Nothing?>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 16
fun case_16(x: Collection<Collection<Collection<Collection<Collection<Collection<Collection<*>>>>>>>?) {
    if (x !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) else { return }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<*>>>>>>> & kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<*>>>>>>>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<*>>>>>>> & kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<*>>>>>>>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 17
fun case_17(x: MutableMap<*, *>?) {
    if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || false) { return }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<*, *> & kotlin.collections.MutableMap<*, *>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<*, *> & kotlin.collections.MutableMap<*, *>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 18
fun case_18(x: MutableMap<out Number, in Number>?) {
    if (false || false || false || x == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<out kotlin.Number, in kotlin.Number> & kotlin.collections.MutableMap<out kotlin.Number, in kotlin.Number>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<out kotlin.Number, in kotlin.Number> & kotlin.collections.MutableMap<out kotlin.Number, in kotlin.Number>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 19
fun case_19(x: Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in Int>>>>>>>?) {
    if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && true) else { return }
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in kotlin.Int>>>>>>>? & kotlin.Nothing?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in kotlin.Int>>>>>>>? & kotlin.Nothing?")!>x<!>.equals(<!DEBUG_INFO_CONSTANT!>x<!>)
}

// TESTCASE NUMBER: 20
fun case_20(x: Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Number>>>>>>>?) {
    if (true && true && true && x !== null) else return
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out kotlin.Number>>>>>>> & Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out kotlin.Number>>>>>>>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out kotlin.Number>>>>>>> & Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out kotlin.Number>>>>>>>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 21
fun <T> case_21(x: T) {
    if (x == null) return
    <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 22
fun <T> case_22(x: T?) {
    if (x === null) return
    <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 23
fun <T> case_23(x: Inv<in T>?) {
    if (x !== null) else return
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 24
fun <T> case_24(x: Inv<out T?>?, y: Nothing?) {
    if (x !== <!DEBUG_INFO_CONSTANT!>y<!> && true) else return
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.equals(x)
}

// TESTCASE NUMBER: 25
fun case_25(x: Int?) {
    val x = (l@ {
        if (x == null) return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
    })()
}

// TESTCASE NUMBER: 26
fun case_26(x: Unit?, y: List<*>) {
    y.forEach l@ {
        if (x === null) return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 27
fun case_27(x: Nothing?) = (l@ {
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) else return@l
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.equals(<!DEBUG_INFO_CONSTANT!>x<!>)
})()

// TESTCASE NUMBER: 28
fun case_28(x: Number?) {
    {(l@ {
        if (x !== null) else { return@l }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    })()}()
}

// TESTCASE NUMBER: 29
fun case_29(x: Char?, y: Nothing?) = l@ {
    if (x != <!DEBUG_INFO_CONSTANT!>y<!>) else return@l
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 30
fun case_30(x: Object?): Any {
    return (l@ {
        if (x !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) else { return@l }
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    })()
}

// TESTCASE NUMBER: 31
fun case_31(x: Class?): Any {
    return l@ {
        if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || false || false || false) { return@l }
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 32
fun case_32(x: Any?) {
    case_32((l@ {
        if (false || false || false || x == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    })())
}

// TESTCASE NUMBER: 33
fun case_33(x: Any?) {
    case_33(l@ {
        if (x != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && true && true && true) else { return@l }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    })
}

// TESTCASE NUMBER: 34
fun case_34(x: Float?) {
    (l@ {
        if (true && true && true && x !== null) else return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }).equals(l@ {
        if (true && true && true && x !== null) else return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    })
}

// TESTCASE NUMBER: 35
fun case_35(x: Any?) {
    case_35 l@ {
        if (x == null) return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 36
fun case_36(x: Any?) {
    case_36 l@ {
        if (x === null) return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 37
fun case_37(x: Any?): Any? = case_37 l@ {
    if (x === null) return@l
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
}

// TESTCASE NUMBER: 38
fun case_39(x: MutableCollection<in Number>?) {
    (l1@ {
        (l2@ {
            if (x !== null) else {
                return@l2
            }
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.equals(x)
        })()
    })()
}

// TESTCASE NUMBER: 39
fun case_39(x: MutableCollection<out Nothing?>?, y: Nothing?) {
    (l2@ {
        if (x != <!DEBUG_INFO_CONSTANT!>y<!>) else return@l2
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<out kotlin.Nothing?> & kotlin.collections.MutableCollection<out kotlin.Nothing?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<out kotlin.Nothing?> & kotlin.collections.MutableCollection<out kotlin.Nothing?>?")!>x<!>.equals(x)
    })()
}

// TESTCASE NUMBER: 40
fun case_40(x: Collection<Collection<Collection<Collection<Collection<Collection<Collection<*>>>>>>>?) {
    val z = (l@ {
        if (x !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) else { return@l }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<*>>>>>>> & kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<*>>>>>>>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<*>>>>>>> & kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<kotlin.collections.Collection<*>>>>>>>?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    })()
}

// TESTCASE NUMBER: 41
fun case_41(x: MutableMap<*, *>?) {
    listOf(l@ {
        if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || false) { return@l }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<*, *> & kotlin.collections.MutableMap<*, *>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<*, *> & kotlin.collections.MutableMap<*, *>?")!>x<!>.equals(x)
    })
}

// TESTCASE NUMBER: 42
fun case_42(x: MutableMap<out Number, in Number>?) {
    return (l@ {
        if (false || false || false || x == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<out kotlin.Number, in kotlin.Number> & kotlin.collections.MutableMap<out kotlin.Number, in kotlin.Number>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<out kotlin.Number, in kotlin.Number> & kotlin.collections.MutableMap<out kotlin.Number, in kotlin.Number>?")!>x<!>.equals(x)
    })()
}

// TESTCASE NUMBER: 43
fun case_43(x: Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in Int>>>>>>>?): Any? {
    return l@ {
        if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && true) else { return@l }
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in kotlin.Int>>>>>>>? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in Inv<in kotlin.Int>>>>>>>? & kotlin.Nothing?")!>x<!>.equals(<!DEBUG_INFO_CONSTANT!>x<!>)
    }
}

// TESTCASE NUMBER: 44
fun case_44(x: Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Number>>>>>>>?) {
    (l@ {
        if (true && true && true && x !== null) else return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out kotlin.Number>>>>>>> & Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out kotlin.Number>>>>>>>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out kotlin.Number>>>>>>> & Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out Inv<out kotlin.Number>>>>>>>?")!>x<!>.equals(x)
    }).invoke()
}

// TESTCASE NUMBER: 45
fun <T> case_45(x: T) {
    val y = (l@ {
        if (x == null) return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    })()
}

// TESTCASE NUMBER: 46
fun <T> case_46(x: T?) {
    (l@ {
        if (x === null) return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    })()
}

// TESTCASE NUMBER: 47
fun <T> case_47(x: Inv<in T>?) {
    val y = l@ {
        if (x !== null) else return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 48
fun <T> case_48(x: Inv<out T?>?, y: Nothing?) {
    val y = ((((l@ {
        if (x !== <!DEBUG_INFO_CONSTANT!>y<!> && true) else return@l
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.equals(x)
    }))))
}
