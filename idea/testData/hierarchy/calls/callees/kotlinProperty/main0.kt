class KA {
    val name = "A"
    fun foo(s: String): String = "A: $s"
}

fun packageFun(s: String): String = s

val packageVal = ""

class KClient() {
    val <caret>bar: String
        get() {
            fun localFun(s: String): String = packageFun(s)
            val localVal = localFun("")

            KA().foo(KA().name)
            JA().foo(JA().name)
            localFun(packageVal)

            run {
                KA().foo(KA().name)
                JA().foo(JA().name)
                packageFun(localVal)
            }

            return ""
        }
}