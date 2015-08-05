interface I {
    fun takeXxx(): Int = 0
    fun takeYyy(): Int = 0
    fun takeZzz(): Int = 0
}

fun foo(i: I) {
    val yyy = i.take<caret>
}

// ORDER: takeYyy
// ORDER: takeXxx
// ORDER: takeZzz
