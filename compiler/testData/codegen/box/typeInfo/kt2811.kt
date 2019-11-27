// IGNORE_BACKEND_FIR: JVM_IR
open class Test1 {
    fun test1(): String {
        if (this is Test2) {
            return this.foo()
        }
        return "fail"
    }
}

class Test2(): Test1() {
    fun foo(): String {
        return "OK"
    }
}

fun Test1.test2(): String {
    if (this is Test2) return this.foo() else return "fail"
}

fun box(): String {
    if ("OK" == Test2().test1() && "OK" == Test2().test2()) {
        return "OK"
    }
    return "fail"
}
