package test

enum class E {
    A;

    companion object {
        public const val A: Int = 1
    }
}

fun main(args: Array<String>) {
    val a = E.Companion.A + 2
}