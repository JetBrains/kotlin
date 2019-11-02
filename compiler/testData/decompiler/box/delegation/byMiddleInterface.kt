interface Base {
    fun getValue(): String
    fun test(): String = getValue()
}

interface BaseKotlin : Base {
}

class Fail : BaseKotlin {
    override fun getValue() = "Fail"
}

fun box(): String {
    val z = object : BaseKotlin by Fail() {
        override fun getValue() = "OK"
    }
    return z.test()
}
