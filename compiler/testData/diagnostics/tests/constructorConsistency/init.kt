// IGNORE_REVERSED_RESOLVE
class My {
    val x: String

    init {
        x = <!DEBUG_INFO_LEAKING_THIS!>foo<!>()
    }

    fun foo(): String = x
}