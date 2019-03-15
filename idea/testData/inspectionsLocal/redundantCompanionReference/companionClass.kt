class Companion {
    fun test() {
        <caret>Companion.foo
    }

    companion object {
        val foo = ""
    }
}