open class KBase {
    open fun foo(s: String): String = s
}

class KA: KBase() {
    val name = "A"
    override fun <caret>foo(s: String): String = "A: $s"
}

fun packageFun(s: String): String = KBase().foo("") + KA().foo(s)

val packageVal = KBase().foo("") + KA().foo("")

class KClient {
    {
        KA().foo("")
        KBase().foo("")
    }

    default object {
        val a = KBase().foo("") + KA().foo("")
    }

    val bar: String
        get() = KBase().foo("") + KA().foo("")

    fun bar() {
        fun localFun() = KBase().foo("") + KA().foo("")

        KA().foo("")
        KBase().foo("")
    }
}

object KClientObj {
    val a = KBase().foo("") + KA().foo("")
}