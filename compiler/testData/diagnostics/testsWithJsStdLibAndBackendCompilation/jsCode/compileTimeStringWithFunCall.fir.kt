// FIR_DIFFERENCE
// The difference is okay: K1 and K2 report a bit differently
// ERROR_POLICY: SEMANTIC
@file:Suppress("NOTHING_TO_INLINE")

fun function1() = "function1"

inline fun inlineFunction1() = "inlineFunction1"

class Class {
    fun method1() = "method1"
    inline fun inlineMethod1() = "method1"

    fun testMethodFromThis() {
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var a = '${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>method1()<!>}' + '" + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>method1()<!> + "';"<!>)
    }

    fun testInlineMethodFromThis() {
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var a = '${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>inlineMethod1()<!>}' + '" + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>inlineMethod1()<!> + "';"<!>)
    }
}

fun testFunction() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var a = '${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>function1()<!>}' + '" + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>function1()<!> + "';"<!>)
}

fun testInlineFunction() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var a = '${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>inlineFunction1()<!>}' + '" + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>inlineFunction1()<!> + "';"<!>)
}

fun testMethod() {
    val c = Class()
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var a = '${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.method1()}' + '" + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.method1() + "';"<!>)
}

fun testInlineMethod() {
    val c = Class()
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var a = '${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.inlineMethod1()}' + '" + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.inlineMethod1() + "';"<!>)
}
