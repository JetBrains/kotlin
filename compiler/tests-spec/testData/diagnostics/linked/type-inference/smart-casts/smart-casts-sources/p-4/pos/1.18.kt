// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
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
 * NUMBER: 18
 * DESCRIPTION: Smartcasts from nullability condition (value or reference equality with `null` and `break` or `continue` after it) using if expression.
 * UNSPECIFIED BEHAVIOR
 * HELPERS: objects, properties, classes
 */

// TESTCASE NUMBER: 1
fun case_1(x: Int?) {
    while (true) {
        if (x == null) break
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Unit?) {
    while (true) {
        if (x === null) continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Nothing?, f: Boolean) {
    do {
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) else break
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.equals(<!DEBUG_INFO_CONSTANT!>x<!>)
    } while (f)
}

// TESTCASE NUMBER: 4
fun case_4(x: Number?) {
    for (i in 0..10) {
        if (x !== null) else { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Char?, y: Nothing?, f: Boolean) {
    do {
        if (x != <!DEBUG_INFO_CONSTANT!>y<!>) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    } while (f)
}

// TESTCASE NUMBER: 6
fun case_6(x: Object?, f: Boolean) {
    while (f) {
        if (x !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) else { continue }
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Class?, list: List<Int>) {
    for (element in list) {
        if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || false || false || false) { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Int?) {
    for (i in 0..10) {
        if (false || false || false || x == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 9
fun case_9(list: List<Int?>) {
    for (element in list) {
        if (element != <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && true && true && true) else { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>element<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>element<!>.inv()
    }
}

// TESTCASE NUMBER: 10
fun case_10(x: Float?) {
    while (false) {
        if (true && true && true && x !== null) else break
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: Out<*>?, list: List<Int>) {
    for (element in list) {
        if (x == null) continue
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 12
fun case_12(list: List<Int?>) {
    for (element in list) {
        if (element === null) continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>element<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>element<!>.inv()
    }
}

// TESTCASE NUMBER: 13
fun case_13(x: Map<out Number, *>?) {
    do {
        if (x != null) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.equals(x)
    } while (false)
}

// TESTCASE NUMBER: 14
fun case_14(x: MutableCollection<in Number>?, r: IntRange) {
    for (i in r) {
        if (x !== null) else { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 15
fun case_15(map: MutableMap<Int?, Int?>, y: Nothing?) {
    for ((k, v) in map) {
        if (k != <!DEBUG_INFO_CONSTANT!>y<!>) else break
        if (v != <!DEBUG_INFO_CONSTANT!>y<!>) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>k<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>k<!>.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.inv()
    }
}

// TESTCASE NUMBER: 16
fun case_16(map: Map<Int?, Int?>) {
    for ((k, v) in map) {
        if (k !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && v !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) else { continue }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>k<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>k<!>.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>v<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>v<!>.inv()
    }
}

// TESTCASE NUMBER: 17
fun <T>case_17(x: T?, f: Boolean) {
    while (f) {
        if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || false) { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 18
fun <T>case_18(x: T, f: Boolean) {
    while (f) {
        if (false || false || false || x == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) break
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 19
fun <K, V>case_19(map: MutableMap<K, V>, y: Nothing?) {
    for ((k, v) in map) {
        if (k !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && true && v != <!DEBUG_INFO_CONSTANT!>y<!>) else { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!")!>k<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!"), DEBUG_INFO_SMARTCAST!>k<!>.equals(k)
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
    }
}

// TESTCASE NUMBER: 20
fun <K, V: K>case_20(map: MutableMap<K?, V>) {
    for ((k, v) in map) {
        if (true && true && true && k !== null && v != null && true) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.equals(k)
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
    }
}

// TESTCASE NUMBER: 21
fun <K, V>case_21(map: MutableMap<out K?, in V>) {
    for ((k, v) in map) {
        if (k == null) continue
        if (v === null || false) break
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.equals(k)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>v<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(v)
    }
}

// TESTCASE NUMBER: 22
fun <T> case_22(x: T?) {
    while (true) {
        if (x === null) break
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 23
fun <T> case_23(x: Inv<in T>?) {
    for (i in -10..10) {
        if (x !== null) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.equals(x)
    }
}

// TESTCASE NUMBER: 24
fun <T> case_24(x: Inv<out T?>?, y: Nothing?) {
    do {
        if (x !== <!DEBUG_INFO_CONSTANT!>y<!> && true) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.equals(x)
    } while (true)
}

// TESTCASE NUMBER: 25
fun <T> case_25(x: Inv<out T?>?, y: Nothing?, z: List<Int>) {
    for (i in z) {
        if (x !== <!DEBUG_INFO_CONSTANT!>y<!>) else try {
            <!UNREACHABLE_CODE!>break<!>
        } finally {
            continue
        }
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.equals(x)
    }
}
