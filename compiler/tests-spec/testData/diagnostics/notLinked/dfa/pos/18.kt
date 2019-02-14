// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 18
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: objects, properties, classes, functions
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Nothing?, f: Boolean) {
    do {
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) else break
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>.hashCode()
    } while (f)
}

// TESTCASE NUMBER: 4
fun case_4(x: Number?) {
    for (i in 0..10) {
        if (x !== null) else { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Char?, y: Nothing?, f: Boolean) {
    do {
        if (x != <!DEBUG_INFO_CONSTANT!>y<!>) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>.funNullableAny()
    } while (f)
}

// TESTCASE NUMBER: 6
fun case_6(x: Object?, f: Boolean) {
    while (f) {
        if (x !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!>) else { continue }
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Object & Object?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Class?, list: List<Int>) {
    for (element in list) {
        if (x === <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> || false || false || false) { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: Out<*>?, list: List<Int>) {
    for (element in list) {
        if (x == null) continue
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<*> & Out<*>?")!>x<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map<out kotlin.Number, *> & kotlin.collections.Map<out kotlin.Number, *>?")!>x<!>.funNullableAny()
    } while (false)
}

// TESTCASE NUMBER: 14
fun case_14(x: MutableCollection<in Number>?, r: IntRange) {
    for (i in r) {
        if (x !== null) else { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableCollection<in kotlin.Number> & kotlin.collections.MutableCollection<in kotlin.Number>?")!>x<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 18
fun <T>case_18(x: T, f: Boolean) {
    while (f) {
        if (false || false || false || x == <!DEBUG_INFO_CONSTANT!>nullableNothingProperty<!>) break
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 19
fun <K, V>case_19(map: MutableMap<K, V>, y: Nothing?) {
    for ((k, v) in map) {
        if (k !== <!DEBUG_INFO_CONSTANT!>implicitNullableNothingProperty<!> && true && v != <!DEBUG_INFO_CONSTANT!>y<!>) else { break }
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!")!>k<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!"), DEBUG_INFO_SMARTCAST!>k<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!"), DEBUG_INFO_SMARTCAST!>k<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!"), DEBUG_INFO_SMARTCAST!>k<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!")!>k<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!")!>k<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!"), DEBUG_INFO_SMARTCAST!>k<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!"), DEBUG_INFO_SMARTCAST!>k<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!")!>k<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("K & K!!")!>k<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 20
fun <K, V: K>case_20(map: MutableMap<K?, V>) {
    for ((k, v) in map) {
        if (true && true && true && k !== null && v != null && true) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!"), DEBUG_INFO_SMARTCAST!>v<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("V & V!!")!>v<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 21
fun <K, V>case_21(map: MutableMap<out K?, in V>) {
    for ((k, v) in map) {
        if (k == null) continue
        if (v === null || false) break
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?"), DEBUG_INFO_SMARTCAST!>k<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("K!! & K?")!>k<!>.funNullableAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>v<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>v<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>v<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>v<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>v<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>v<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>v<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>v<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>v<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>v<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 22
fun <T> case_22(x: T?) {
    while (true) {
        if (x === null) break
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?"), DEBUG_INFO_SMARTCAST!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 23
fun <T> case_23(x: Inv<in T>?) {
    for (i in -10..10) {
        if (x !== null) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in T> & Inv<in T>?")!>x<!>.funNullableAny()
    }
}

// TESTCASE NUMBER: 24
fun <T> case_24(x: Inv<out T?>?, y: Nothing?) {
    do {
        if (x !== <!DEBUG_INFO_CONSTANT!>y<!> && true) else continue
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.funNullableAny()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.equals(null)
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.propT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.propAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.propNullableT
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.propNullableAny
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.funT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.funAny()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.funNullableT()
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out T?> & Inv<out T?>?")!>x<!>.funNullableAny()
    }
}
