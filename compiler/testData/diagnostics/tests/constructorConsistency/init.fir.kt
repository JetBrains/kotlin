// IGNORE_REVERSED_RESOLVE
class My {
    val x: String

    init {
        x = foo()
    }

    fun foo(): String = x
}