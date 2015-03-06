class <caret>KA {
    val name = "A"
    fun foo(s: String): String = "A: $s"
}

fun packageFun(s: String): String = s + KA().name

val packageVal = KA().name

class KClient {
    {
        KA()
    }

    default object {
        val a = KA()
    }

    val bar: String
        get() = KA().name

    fun bar() {
        fun localFun() = KA()

        KA()
    }
}

object KClientObj {
    val a = KA()
}