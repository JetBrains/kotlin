class T {
    inner class <caret>KA {
        val name = "A"
        fun foo(s: String): String = "A: $s"
    }
}

fun packageFun(s: String): String = s + T().KA().name

val packageVal = T().KA().name

class KClient {
    init {
        T().KA()
    }

    companion object {
        val a = T().KA()
    }

    val bar: String
        get() = T().KA().name

    fun bar() {
        fun localFun() = T().KA()

        T().KA()
    }
}

object KClientObj {
    val a = T().KA()
}