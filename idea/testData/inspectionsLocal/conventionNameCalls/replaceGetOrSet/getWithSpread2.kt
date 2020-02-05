// PROBLEM: none
class B {
    operator fun get(vararg args: String) {
        println(args.size)
    }

    private fun println(i: Int) {}
}

fun main() {
    val args = arrayOf("a", "b")
    B().<caret>get(*args)
}
