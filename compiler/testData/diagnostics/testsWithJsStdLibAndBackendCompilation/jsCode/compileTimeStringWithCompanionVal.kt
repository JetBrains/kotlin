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
            js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!>} = 1;")
            js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!> + " = 1;")
        }
    }

    fun testCompanionFromThis() {
        js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!>} = 1;")
        js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!> + " = 1;")
    }
}

class ClassFromThisFile2 {
    companion object {
        val valFromThisFileCompanion2 = ClassFromThisFile.valFromThisFileCompanion + "2"

        fun testCompanionFromThisCompanion() {
            js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!>} = 1;")
            js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!> + " = 1;")
        }
    }

    fun testCompanionFromThis() {
        js("var ${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!>} = 1;")
        js("var " + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!> + " = 1;")
    }
}

fun testCompanionValFromOtherModule() {
    js("var ${ClassFromOtherModule.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModuleCompanion<!>} = 1;")
    js("var " + ClassFromOtherModule.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModuleCompanion<!> + " = 1;")
}

fun testCompanionValFromOtherModule2() {
    js("var ${ClassFromOtherModule2.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModuleCompanion2<!>} = 1;")
    js("var " + ClassFromOtherModule2.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherModuleCompanion2<!> + " = 1;")
}


fun testCompanionValFromOtherFile() {
    js("var ${ClassFromOtherFile.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFileCompanion<!>} = 1;")
    js("var " + ClassFromOtherFile.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFileCompanion<!> + " = 1;")
}

fun testCompanionValFromOtherFile2() {
    js("var ${ClassFromOtherFile2.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFileCompanion2<!>} = 1;")
    js("var " + ClassFromOtherFile2.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromOtherFileCompanion2<!> + " = 1;")
}

fun testCompanionValFromThisFile() {
    js("var ${ClassFromThisFile.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!>} = 1;")
    js("var " + ClassFromThisFile.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion<!> + " = 1;")
}

fun testCompanionValFromThisFile2() {
    js("var ${ClassFromThisFile2.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!>} = 1;")
    js("var " + ClassFromThisFile2.<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>valFromThisFileCompanion2<!> + " = 1;")
}
