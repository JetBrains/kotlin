fun <caret>f(): List<String> = emptyList()

fun main(args: Array<String>) {
    val list1 = f()
    val list2: List<String> = f()
}