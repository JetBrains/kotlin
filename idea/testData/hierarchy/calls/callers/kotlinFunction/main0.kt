class KA {
    val name = "A"
    fun <caret>foo(s: String): String = "A: $s"
}

fun packageFun(s: String): String = KA().foo(s)

val packageVal = KA().foo("")

class KClient {
    {
        KA().foo("")
    }

    class object {
        val a = KA().foo("")
    }

    val bar: String
        get() = KA().foo("")

    fun bar() {
        fun localFun() = KA().foo("")

        KA().foo("")
    }
}

object KClientObj {
    val a = KA().foo("")
}