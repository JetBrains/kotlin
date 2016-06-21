interface Z {
    companion object {
        val instance: Z? = null
    }
}

fun foo(): Z? = Z<caret>

// ORDER: instance
// ORDER: object