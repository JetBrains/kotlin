fun test() {
    class Test{
        fun contains(fn: () -> Boolean) : Boolean = true
    }
    val test = Test()
    if (true) {
        test.c<caret>ontains {
            true
        }
    }
}
