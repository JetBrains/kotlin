fun test() {
    class Test{
        fun contains(fn: () -> Boolean) : Boolean = true
    }
    val test = Test()
    println(test.c<caret>ontains { true }.toString())
}
