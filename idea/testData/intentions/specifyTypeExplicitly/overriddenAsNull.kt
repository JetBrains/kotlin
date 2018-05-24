// DO_NOT_CHOOSE_NOTHING

interface I {
    fun foo(): String?
}

class Test : I {
    override fun foo()<caret> = null
}
