class K {
    fun reverse(s: String): String {
        return s.reversed()
    }

    companion object {
        fun getRef() = K::reverse
    }
}

fun box(): String {
    return J.go()
}
