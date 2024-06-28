// FIR_DIFFERENCE
// The difference is okay: K1 and K2 report a bit differently

// MODULE: lib1
// FILE: A.kt
class ClassFromOtherModule {
    companion object {
        val valFromOtherModuleCompanion = "valFromOtherModuleCompanion"
    }
}

// FILE: B.kt
class ClassFromOtherModule2 {
    companion object {
        val valFromOtherModuleCompanion2 = ClassFromOtherModule.valFromOtherModuleCompanion + "2"
    }
}

// MODULE: main(lib1)
// FILE: A.kt
class ClassFromOtherFile {
    companion object {
        val valFromOtherFileCompanion = "valFromOtherFileCompanion"
    }
}

// FILE: B.kt
class ClassFromOtherFile2 {
    companion object {
        val valFromOtherFileCompanion2 = ClassFromOtherFile.valFromOtherFileCompanion + "2"
    }
}

// FILE: Main.kt
class ClassFromThisFile {
    companion object {
        val valFromThisFileCompanion = "valFromThisFileCompanion"

        fun testCompanionFromThisCompanion() {
            js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!>} = 1;"<!>)
            js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!> + " = 1;"<!>)
        }
    }

    fun testCompanionFromThis() {
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!>} = 1;"<!>)
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!> + " = 1;"<!>)
    }
}

class ClassFromThisFile2 {
    companion object {
        val valFromThisFileCompanion2 = ClassFromThisFile.valFromThisFileCompanion + "2"

        fun testCompanionFromThisCompanion() {
            js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!>} = 1;"<!>)
            js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!> + " = 1;"<!>)
        }
    }

    fun testCompanionFromThis() {
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!>} = 1;"<!>)
        js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!> + " = 1;"<!>)
    }
}

fun testCompanionValFromOtherModule() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromOtherModule.valFromOtherModuleCompanion<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromOtherModule.valFromOtherModuleCompanion<!> + " = 1;"<!>)
}

fun testCompanionValFromOtherModule2() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromOtherModule2.valFromOtherModuleCompanion2<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromOtherModule2.valFromOtherModuleCompanion2<!> + " = 1;"<!>)
}


fun testCompanionValFromOtherFile() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromOtherFile.valFromOtherFileCompanion<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromOtherFile.valFromOtherFileCompanion<!> + " = 1;"<!>)
}

fun testCompanionValFromOtherFile2() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromOtherFile2.valFromOtherFileCompanion2<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromOtherFile2.valFromOtherFileCompanion2<!> + " = 1;"<!>)
}

fun testCompanionValFromThisFile() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromThisFile.valFromThisFileCompanion<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromThisFile.valFromThisFileCompanion<!> + " = 1;"<!>)
}

fun testCompanionValFromThisFile2() {
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromThisFile2.valFromThisFileCompanion2<!>} = 1;"<!>)
    js(<!JSCODE_CAN_NOT_VERIFY_JAVASCRIPT!>"var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>ClassFromThisFile2.valFromThisFileCompanion2<!> + " = 1;"<!>)
}
