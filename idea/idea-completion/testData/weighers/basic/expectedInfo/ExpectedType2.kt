interface I {
    fun takeXxx(): Int = 0
    fun takeYyy(): String = ""
    fun takeZzz(): Int = 0
}

fun foo(i: I): String {
    return i.take<caret>
}

// ORDER: takeYyy
// ORDER: takeXxx
// ORDER: takeZzz
