interface X {
    fun foo(): String
    val bar: String
    fun qux(): String
}

interface Z {
    fun qux(): String = "initially default method"
}

