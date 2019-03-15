fun foo(): Int = 42

// !
fun bar() = 42

fun baz() {
    fun bar() = 42
}

class My {
    fun foo(): Int = 42

    // !
    fun bar() = 42

    // !
    protected val x = ""

    // !
    val y get() = true

    private val z = 0
}

private class Your {
    fun bar() = 42

    val x = ""
}