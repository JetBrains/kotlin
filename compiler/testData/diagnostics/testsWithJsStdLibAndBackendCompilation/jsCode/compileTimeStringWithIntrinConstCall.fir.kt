// FIR_DIFFERENCE
// The difference is okay: K1 and K2 report a bit differently
// ERROR_POLICY: SEMANTIC

@file:Suppress(
    "DEPRECATED_IDENTITY_EQUALS",
    "SENSELESS_COMPARISON"
)

fun testTrimMargin() {
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>"""
    |   var x = 1;
    """.<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>trimMargin()<!><!>)
}

fun testTrimIndent() {
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>"""
        var x = 1;
    """.<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>trimIndent()<!><!>)
}

const val ONE = 1
const val UONE = 1U
const val HALF = 0.5
const val TRUE = true
const val STR = "str"
const val CHAR = 'C'

fun testStringSize() {
    js("var a = ${STR.length};")
}

fun testSimpleStringConcat() {
    js("{ var a = '${'b'}'; }")
    js("{ var a = ${123}; }")
    js("{ var a = ${123U}; }")
    js("{ var a = ${123L}; }")
    js("{ var a = ${123UL}; }")
    js("{ var a = ${1.23}; }")
    js("{ var a = ${1.23f}; }")
    js("{ var a = ${true}; }")
    js("{ var a = ${false}; }")
    js("{ var a = ${null}; }")

    js("{ var a = ${ONE}; }")
    js("{ var a = ${UONE}; }")
    js("{ var a = ${HALF}; }")
    js("{ var a = ${TRUE}; }")
    js("{ var a = '${STR}'; }")
    js("{ var a = '${CHAR}'; }")
}

fun testArithmeticOperations() {
    js("{ var a = ${1 + 2}; }")
    js("{ var a = ${1 - 2}; }")
    js("{ var a = ${1 * 2}; }")
    js("{ var a = ${1 / 2}; }")
    js("{ var a = ${1 % 2}; }")
    js("{ var a = ${1.1 + 2.1}; }")
    js("{ var a = ${1.1 - 2.1}; }")
    js("{ var a = ${1.1 * 2.1}; }")
    js("{ var a = ${1.1 / 2.1}; }")

    js("{ var a = ${ONE + 2}; }")
    js("{ var a = ${HALF + 2.1}; }")

    js("{ var a = '${"foo" + "bar"}'; }")
    js("{ var a = '${"foo" + 'c'}'; }")
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = '${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>'c' + "foo"<!>}'; }"<!>)

    js("{ var a = ${STR + STR}; }")
    js("{ var a = ${STR + CHAR}; }")
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>CHAR + STR<!>}; }"<!>)
}

fun testLogicOperations() {
    js("{ var a = ${!true}; }")
    js("{ var a = ${true or false}; }")
    js("{ var a = ${true || false}; }")
    js("{ var a = ${true and false}; }")
    js("{ var a = ${true && false}; }")

    js("{ var a = ${TRUE && false}; }")
    js("{ var a = ${TRUE or false}; }")
}

fun testEq() {
    js("{ var a = ${1 == 1}; }")
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>1U == 1U<!>}; }"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>UONE == 1U<!>}; }"<!>)
    js("{ var a = ${"FOO" == STR}; }")
    js("{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>TRUE == null<!>}; }")
    js("{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>STR == null<!>}; }")

    js("{ var a = ${1 != 1}; }")
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>1U != 1U<!>}; }"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>UONE != 1U<!>}; }"<!>)
    js("{ var a = ${"FOO" != STR}; }")
    js("{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>TRUE != null<!>}; }")
    js("{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>STR != null<!>}; }")

    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>1 === 1<!>}; }"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>TRUE === false<!>}; }"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>CHAR === 's'<!>}; }"<!>)

    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>1 !== 1<!>}; }"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>TRUE !== false<!>}; }"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"{ var a = ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>CHAR !== 's'<!>}; }"<!>)
}

fun testCmp() {
    js("{ var a = ${1 < 1}; }")
    js("{ var a = ${1 <= 1}; }")
    js("{ var a = ${1 > 1}; }")
    js("{ var a = ${1 >= 1}; }")

    js("{ var a = ${"fo=" < "bar"}; }")
    js("{ var a = ${'a' > 'c'}; }")

    js("{ var a = ${ONE > 1}; }")
    js("{ var a = ${STR <= "1"}; }")
}
