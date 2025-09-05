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
            <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>test<!>()
            s.<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>test2<!>()
            with(s) {
                <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>test2<!>()
            }

            val x: String = test3()
            <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>test4<!>()
        }

        inner class C {
            fun t() {
                <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>test<!>()
                "".<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>test2<!>()
                <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>test4<!>()
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, lambdaLiteral, nestedClass */
