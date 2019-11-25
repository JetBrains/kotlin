// IGNORE_BACKEND_FIR: JVM_IR
open class M() {
    open var y = 500
}

open class N() : M() {

    override var y = 200

    open inner class C() {
        fun test5() = y
        fun test6() : Int {
            super<M>@N.y += 200
            return super<M>@N.y
        }
    }
}

fun box(): String {
    if (N().C().test5() != 200) return "test5 fail";
    if (N().C().test6() != 700) return "test6 fail";
    return "OK";
}
