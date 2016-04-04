// IS_APPLICABLE: false

fun test() {
    class Test{
        fun contains(a: Int) : Boolean = true
    }
    val test = Test()
    test.c<caret>ontains(0)
}