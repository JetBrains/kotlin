// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 36
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, functions, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1() {
    var a: Any? = null

    if (a == null) return

    val b = select(<!DEBUG_INFO_SMARTCAST!>a<!>)
    val c = a

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.equals(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>c<!>.equals(10)
}

// TESTCASE NUMBER: 2
fun case_2(x: Any) {
    if (x is String) {
        val y = x
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String"), DEBUG_INFO_SMARTCAST!>y<!>.length
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x is Number?) {
        var y = x
        if (y == null) throw Exception()
        var z = y
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Number")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Number")!>z<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>y<!>.toByte()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>z<!>.toByte()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?) {
    if (x is Number?) {
        var y = x
        while (true && y != null) {
            var z = y
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Number")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Number")!>z<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>y<!>.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>z<!>.toByte()
        }
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    var y = x
    while (false || y != null) {
        if (y is Number) {
            val z = select(<!DEBUG_INFO_SMARTCAST!>y<!>)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Number")!>y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>z<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number"), DEBUG_INFO_SMARTCAST!>y<!>.toByte()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>z<!>.toByte()
        }
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    var y = x ?: null!!
    while (false || y is Number) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Number")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Number"), DEBUG_INFO_SMARTCAST!>y<!>.toByte()
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Any?, z: Any) {
    var y = x ?: null!!
    while (false || y === z) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Any?, z: Any) {
    var y = x ?: null!!
    y == z || return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?, z: Any) {
    var y = select(x) ?: return null!!
    z == y || throw null!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 10
fun case_10(x: Any?, z: Any, b: Boolean?) {
    var y = x ?: <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (b) {
        true -> null!!
        false -> return
        null -> throw Exception()
    }<!>
    z === y || if (b == true) return else if (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>b === false<!>) null!! else throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 11
fun case_11(x: Any?, z: Any, b: Boolean?) {
    while (true) {
        var y = x ?: if (b == true) continue<!UNNECESSARY_NOT_NULL_ASSERTION, UNREACHABLE_CODE!>!!<!> else if (!(b != false)) return else break <!UNREACHABLE_CODE, USELESS_ELVIS!>?: break::class<!>
        z !== y && if (b == true) return else if (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>b === false<!>) null!!else throw Exception()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: Any?, z: Any, b: Boolean?) {
    while (true) {
        var y = select(x) ?: if (b == true) continue<!UNNECESSARY_NOT_NULL_ASSERTION, UNREACHABLE_CODE!>!!<!> else if (!(b != false)) return else break <!UNREACHABLE_CODE, USELESS_ELVIS!>?: break::class<!>
        select(z) !== y && if (b == true) return else if (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>b === false<!>) null!!else throw Exception()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
    }
}

// TESTCASE NUMBER: 13
fun case_13(x: Any?) {
    if (x is Number?) {
        var y = select(select(select(select(select(select(<!DEBUG_INFO_SMARTCAST!>x<!>))))))
        if (y == null) throw Exception()
        var z = select(select(select(select(<!DEBUG_INFO_SMARTCAST!>y<!>), select(<!DEBUG_INFO_SMARTCAST!>y<!>)), select(select(<!DEBUG_INFO_SMARTCAST!>y<!>), select(<!DEBUG_INFO_SMARTCAST!>y<!>))), select(select(select(<!DEBUG_INFO_SMARTCAST!>y<!>), select(<!DEBUG_INFO_SMARTCAST!>y<!>)), select(select(<!DEBUG_INFO_SMARTCAST!>y<!>), select(<!DEBUG_INFO_SMARTCAST!>y<!>))))
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>z<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!>y<!>.toByte()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>z<!>.toByte()
    }
}

// TESTCASE NUMBER: 14
fun case_14(x: Any?) {
    if (x is Number?) {
        var y = removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>y<!>.toByte()
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: Any?) {
    if (x is Number?) {
        var y = removeNullable(removeNullable(removeNullable(removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>), removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>)), removeNullable(removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>), removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>))), removeNullable(removeNullable(removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>), removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>)), removeNullable(removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>), removeNullable(<!DEBUG_INFO_SMARTCAST!>x<!>))))
        if (y !is Int) throw Exception()
        var z = select(select(select(select(<!DEBUG_INFO_SMARTCAST!>y<!>), select(<!DEBUG_INFO_SMARTCAST!>y<!>)), select(select(<!DEBUG_INFO_SMARTCAST!>y<!>), select(<!DEBUG_INFO_SMARTCAST!>y<!>))), select(select(select(<!DEBUG_INFO_SMARTCAST!>y<!>), select(<!DEBUG_INFO_SMARTCAST!>y<!>)), select(select(<!DEBUG_INFO_SMARTCAST!>y<!>), select(<!DEBUG_INFO_SMARTCAST!>y<!>))))
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>z<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number"), DEBUG_INFO_SMARTCAST!>y<!>.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>z<!>.inv()
    }
}