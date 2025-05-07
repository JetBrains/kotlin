// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

class Lib {
    val anon = object {
        fun f1() = ""
        fun f2(): String {
            <!RETURN_VALUE_NOT_USED!>f1()<!>
            return ""
        }
    }

    fun anon(): Any {
        val x = object {
            fun f3() = ""
            fun f4(): String {
                <!RETURN_VALUE_NOT_USED!>f3()<!>
                return ""
            }
        }
        <!RETURN_VALUE_NOT_USED!>x.f3()<!>
        <!RETURN_VALUE_NOT_USED!>x.f4()<!>
        return x
    }

    fun outer2() {
        fun f5(): String = ""
        <!RETURN_VALUE_NOT_USED!>f5()<!>
    }
}

fun outer1() {
    fun f6(): String = ""
    <!RETURN_VALUE_NOT_USED!>f6()<!>
}
