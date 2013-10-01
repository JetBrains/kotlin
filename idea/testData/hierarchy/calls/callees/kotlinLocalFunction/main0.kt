class KA {
    val name = "A"
    fun foo(s: String): String = "A: $s"
}

fun packageFun(s: String): String = s

val packageVal = ""

fun client() {
    fun <caret>localFun(s: String): String {
        val bar = run {
            val localVal = packageFun("")

            KA().foo(KA().name)
            JA().foo(JA().name)
            packageFun(localVal)
        }

        fun bar() {
            KA().foo(KA().name)
            JA().foo(JA().name)
        }

        bar()
    }

    KA().foo(KA().name)
    JA().foo(JA().name)
    localFun(packageVal)
}