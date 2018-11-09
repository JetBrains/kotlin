// "Change return type of enclosing function 'test' to 'String'" "true"
fun test(i: Int) {
    return when (i) {
        0 -> {
            ""
        }
        else -> {
            ""<caret>
        }
    }
}