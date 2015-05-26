class K {
    fun reverse(s: String): String {
        return s.reverse()
    }

    companion object {
        fun getRef() = K::reverse
    }
}

fun box(): String {
    return J.go()
}
