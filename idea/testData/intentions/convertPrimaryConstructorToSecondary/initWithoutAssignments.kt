fun println(arg: Int) = arg

class My<caret>() {
    val x = 1

    val y = 2

    init {
        println(x)
        println(y)
    }
}