fun <R> run(block: () -> R) = block()
inline fun <R> inlineRun(block: () -> R) = block()

class Outer(val outerProp: String) {
    fun foo(arg: String): String {
        class Local {
            val work1 = run { outerProp + arg }
            val work2 = inlineRun { outerProp + arg }
            val obj = object : Any() {
                override fun toString() = outerProp + arg
            }

            override fun toString() = "${work1}#${work2}#${obj.toString()}"
        }

        return Local().toString()
    }
}

fun box(): String {
    val res = Outer("O").foo("K")
    if (res != "OK#OK#OK") return "fail: $res"
    return "OK"
}
