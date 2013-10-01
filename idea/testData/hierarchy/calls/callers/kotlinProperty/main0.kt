class KA {
    var <caret>name = "A"
    fun foo(s: String): String = "A: $s"
}

fun packageFun(s: String): String = KA().name

val packageVal = KA().name

class KClient {
    {
        KA().name = ""
    }

    class object {
        val a = KA().name
    }

    val bar: String
        get() = KA().name
        set(value: String) {KA().name = value}

    fun bar() {
        fun localFun() = KA().name

        val s = KA().name
    }
}

object KClientObj {
    val a = KA().name
    {
        KA().name = ""
    }
}