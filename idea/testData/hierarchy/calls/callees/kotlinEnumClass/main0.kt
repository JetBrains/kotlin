class KA {
    val name = "A"
    fun foo(s: String): String = "A: $s"
}

fun packageFun(s: String): String = s

val packageVal = ""

open class KClientBase {

}

enum class MyEnum {
    <caret>FIRST {
        val bar = run {
            val localVal = packageFun("")

            KA().foo(KA().name)
            JA().foo(JA().name)
            packageFun(localVal)
        }
    }

    SECOND {
        {
            KA().foo(KA().name)
            JA().foo(JA().name)
        }
    }

    fun bar() {
        KA().foo(KA().name)
        JA().foo(JA().name)
    }

    {
        fun localFun(s: String): String = packageFun(s)


        KA().foo(KA().name)
        JA().foo(JA().name)
        localFun(packageVal)
    }
}