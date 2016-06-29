// "Create extension function 'List<Int>.foo'" "true"

open class A

fun main(args: Array<String>) {
    class Local : A()

    val list = listOf(1, 2, 4, 5)
    list.<caret>foo { Local() }
}