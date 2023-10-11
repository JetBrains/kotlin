// FIR_DIFFERENCE
// The difference is okay: K1 and K2 report a bit differently
// ERROR_POLICY: SEMANTIC

@file:Suppress(
    "DEPRECATED_IDENTITY_EQUALS",
    "SENSELESS_COMPARISON"
)

fun testTrimMargin() {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"""
    |   var x = 1;
    """.trimMargin()<!>)
}

fun testTrimIndent() {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"""
        var x = 1;
    """.trimIndent()<!>)
}

const val ONE = 1
const val UONE = 1U
const val HALF = 0.5
const val TRUE = true
const val STR = "str"
const val CHAR = 'C'

fun testStringSize() {
    js("var a = ${STR.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>length<!>};")
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
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = '${'c' + "foo"}'; }"<!>)

    js("{ var a = ${STR + STR}; }")
    js("{ var a = ${STR + CHAR}; }")
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${CHAR + STR}; }"<!>)
}

fun testLogicOperations() {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${!true}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${true or false}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${true || false}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${true and false}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${true && false}; }"<!>)

    js("{ var a = ${TRUE && false}; }")
    js("{ var a = ${TRUE or false}; }")
}

fun testEq() {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1 == 1}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1U == 1U}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${UONE == 1U}; }"<!>)
    js("{ var a = ${"FOO" == STR}; }")
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${TRUE == null}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${STR == null}; }"<!>)

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1 != 1}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1U != 1U}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${UONE != 1U}; }"<!>)
    js("{ var a = ${"FOO" != STR}; }")
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${TRUE != null}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${STR != null}; }"<!>)

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1 === 1}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${TRUE === false}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${CHAR === 's'}; }"<!>)

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1 !== 1}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${TRUE !== false}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${CHAR !== 's'}; }"<!>)
}

fun testCmp() {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1 < 1}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1 <= 1}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1 > 1}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${1 >= 1}; }"<!>)

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${"fo=" < "bar"}; }"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"{ var a = ${'a' > 'c'}; }"<!>)

    js("{ var a = ${ONE > 1}; }")
    js("{ var a = ${STR <= "1"}; }")
}
