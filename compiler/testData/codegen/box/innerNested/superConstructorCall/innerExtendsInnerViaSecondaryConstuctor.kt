// IGNORE_BACKEND_FIR: JVM_IR
open class Father(val param: String) {
    abstract inner class InClass {
        fun work(): String {
            return param
        }
    }

    inner class Child(p: String) : Father(p) {
        inner class Child2 : Father.InClass {
            constructor(): super()
        }
    }
}

fun box(): String {
    return Father("fail").Child("OK").Child2().work()
}
