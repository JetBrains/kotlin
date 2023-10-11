// FIR_DIFFERENCE
// The difference is okay: K1 and K2 report a bit differently
// ERROR_POLICY: SEMANTIC

fun testValFromThisFunction() {
    val valFromThisFunction = "valFromThisFunction"
    val valFromThisFunction2 = valFromThisFunction + "2"

    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFunction<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFunction<!> + " = 1;"<!>)

    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFunction2<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFunction2<!> + " = 1;"<!>)
}

fun testVarFromThisFunction() {
    var varFromThisFunction = "varFromThisFunction"
    var varFromThisFunction2 = varFromThisFunction + "2"

    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>varFromThisFunction<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>varFromThisFunction<!> + " = 1;"<!>)

    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>varFromThisFunction2<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>varFromThisFunction2<!> + " = 1;"<!>)
}

fun testValFromParam(valFromParam: String) {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromParam<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromParam<!> + " = 1;"<!>)
}

class Class {
    val valFromClass = "valFromClass"

    val valWithGetter: String get() = "valWithGetter"

    fun testValFromThis() {
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromClass<!>} = 1;"<!>)
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromClass<!> + " = 1;"<!>)
    }

    fun testValWithGetterThis() {
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valWithGetter<!>} = 1;"<!>)
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valWithGetter<!> + " = 1;"<!>)
    }
}

fun testValFromClassObject() {
    val c = Class()
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.valFromClass} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.valFromClass + " = 1;"<!>)

    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.valWithGetter} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.valWithGetter + " = 1;"<!>)
}

fun testValFromObject() {
    val o = object {
        val valFromObject = "valFromClass"
    }
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>o<!>.valFromObject} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>o<!>.valFromObject + " = 1;"<!>)
}
