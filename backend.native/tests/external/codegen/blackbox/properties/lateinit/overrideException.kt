// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

interface Intf {
    val str: String
}

class A : Intf {
    override lateinit var str: String

    fun getMyStr(): String {
        try {
            val a = str
        } catch (e: RuntimeException) {
            return "OK"
        }
        return "FAIL"
    }
}

fun box(): String {
    val a = A()
    return a.getMyStr()
}