// PROBLEM: none

class Foo : Bar() {
    override fun getBar(): String {
        return <caret>Companion.bar
    }

    companion object {
        val bar: String = "bar"
    }
}