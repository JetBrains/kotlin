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
    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModule<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModule<!> + " = 1;")
}

fun testValFromOtherModule2() {
    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModule2<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModule2<!> + " = 1;")
}

fun testValFromOtherFile() {
    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFile<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFile<!> + " = 1;")
}

fun testValFromOtherFile2() {
    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFile2<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFile2<!> + " = 1;")
}

fun testValFromThisFile() {
    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFile<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFile<!> + " = 1;")
}

fun testValFromThisFile2() {
    js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFile2<!>} = 1;")
    js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFile2<!> + " = 1;")
}

fun testValWithGetter() {
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var ${valWithGetter} = 1;"<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>"var " + valWithGetter + " = 1;"<!>)
}
