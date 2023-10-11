// FIR_DIFFERENCE
// The difference is okay: K1 and K2 report a bit differently
// ERROR_POLICY: SEMANTIC

// MODULE: lib1
// FILE: A.kt
val valFromOtherModule = "valFromOtherModule"

// FILE: B.kt
val valFromOtherModule2 = valFromOtherModule + "2"

// MODULE: main(lib1)
// FILE: A.kt
val valFromOtherFile = "valFromOtherFile"

// FILE: B.kt
val valFromOtherFile2 = valFromOtherFile + "2"

// FILE: Main.kt
val valFromThisFile = "valFromThisFile"
val valFromThisFile2 = valFromThisFile + "2"

val valWithGetter: String get() = "valWithGetter"

fun testValFromOtherModule() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModule<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModule<!> + " = 1;"<!>)
}

fun testValFromOtherModule2() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModule2<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModule2<!> + " = 1;"<!>)
}

fun testValFromOtherFile() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFile<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFile<!> + " = 1;"<!>)
}

fun testValFromOtherFile2() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFile2<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFile2<!> + " = 1;"<!>)
}

fun testValFromThisFile() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFile<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFile<!> + " = 1;"<!>)
}

fun testValFromThisFile2() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFile2<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFile2<!> + " = 1;"<!>)
}

fun testValWithGetter() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valWithGetter<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valWithGetter<!> + " = 1;"<!>)
}
