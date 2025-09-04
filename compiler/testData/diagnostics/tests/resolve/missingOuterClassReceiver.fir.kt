// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-9111

fun test3() = ""

fun A.test4() {}

class A {
    fun test() {}
    fun String.test2() {}
    fun test3() = 1

    class B {
        fun t(s: String) {
            <!UNRESOLVED_REFERENCE!>test<!>()
            s.<!UNRESOLVED_REFERENCE!>test2<!>()
            <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(s) {
                <!UNRESOLVED_REFERENCE!>test2<!>()
            }

            val x: String = test3()
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>test4<!>()
        }

        inner class C {
            fun t() {
                <!UNRESOLVED_REFERENCE!>test<!>()
                "".<!UNRESOLVED_REFERENCE!>test2<!>()
                <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>test4<!>()
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, lambdaLiteral, nestedClass */
