fun bar(): List<Int> = listOf()

fun ff() {
    val copy: List<Int> = ArrayList(<caret>bar())
}
