// FIR_DIFFERENCE
// The difference is okay: K1 and K2 report a bit differently
// ERROR_POLICY: SEMANTIC

fun testValFromThisFunction() {
    val valFromThisFunction = "valFromThisFunction"
    val valFromThisFunction2 = valFromThisFunction + "2"

    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFunction<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFunction<!> + " = 1;")

    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFunction2<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFunction2<!> + " = 1;")
}

fun testVarFromThisFunction() {
    var varFromThisFunction = "varFromThisFunction"
    var varFromThisFunction2 = varFromThisFunction + "2"

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var ${varFromThisFunction} = 1;"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var " + varFromThisFunction + " = 1;"<!>)

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var ${varFromThisFunction2} = 1;"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var " + varFromThisFunction2 + " = 1;"<!>)
}

fun testValFromParam(valFromParam: String) {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var ${valFromParam} = 1;"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var " + valFromParam + " = 1;"<!>)
}

class Class {
    val valFromClass = "valFromClass"

    val valWithGetter: String get() = "valWithGetter"

    fun testValFromThis() {
        js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromClass<!>} = 1;")
        js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromClass<!> + " = 1;")
    }

    fun testValWithGetterThis() {
        js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var ${valWithGetter} = 1;"<!>)
        js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var " + valWithGetter + " = 1;"<!>)
    }
}

fun testValFromClassObject() {
    val c = Class()
    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromClass<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>c<!>.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromClass<!> + " = 1;")

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var ${c.valWithGetter} = 1;"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var " + c.valWithGetter + " = 1;"<!>)
}

fun testValFromObject() {
    val o = object {
        val valFromObject = "valFromClass"
    }
    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>o<!>.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromObject<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>o<!>.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromObject<!> + " = 1;")
}
