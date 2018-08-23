// WITH_RUNTIME

class Host {
    val ok = "OK"

    fun foo() = run { bar(ok) }

    companion object {
        val ok = 0

        fun bar(s: String) = s.substring(ok)
    }
}

fun box() = Host().foo()