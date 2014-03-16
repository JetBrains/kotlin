fun test() {
    class Test{
        fun contains(a: Int) : Boolean = true
    }
    val test = Test()
    println(test.c<caret>ontains(0).toString())
}
