// IGNORE_BACKEND_FIR: JVM_IR
interface Intf {
    val str: String
}

class A : Intf {
    override lateinit var str: String

    fun setMyStr() {
        str = "OK"
    }

    fun getMyStr(): String {
        return str
    }
}

fun box(): String {
    val a = A()
    a.setMyStr()
    return a.getMyStr()
}