// IGNORE_BACKEND_FIR: JVM_IR
abstract class Father {
    abstract inner class InClass {
        abstract fun work(): String
    }
}

class Child : Father() {
    fun test(): InClass {
        return object : Father.InClass() {
            override fun work(): String {
                return "OK"
            }
        }
    }
}

fun box(): String {
    return Child().test().work()
}