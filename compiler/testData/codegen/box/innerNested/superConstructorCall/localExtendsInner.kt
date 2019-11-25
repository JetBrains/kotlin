// IGNORE_BACKEND_FIR: JVM_IR
open class Father(val param: String) {
    abstract inner class InClass {
        fun work(): String {
            return param
        }
    }

    inner class Child(p: String) : Father(p) {
        fun test(): InClass {
            class Local : Father.InClass() {

            }
            return Local()
        }

    }
}

fun box(): String {
    return Father("fail").Child("OK").test().work()
}