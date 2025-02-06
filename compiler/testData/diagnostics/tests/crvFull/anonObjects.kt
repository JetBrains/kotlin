// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

class Lib {
    val anon = object {
        fun f1() = ""
        fun f2(): String {
            f1()
            return ""
        }
    }

    fun anon(): Any {
        val x = object {
            fun f3() = ""
            fun f4(): String {
                f3()
                return ""
            }
        }
        x.f3()
        x.f4()
        return x
    }

    fun outer2() {
        fun f5(): String = ""
        f5()
    }
}

fun outer1() {
    fun f6(): String = ""
    f6()
}
