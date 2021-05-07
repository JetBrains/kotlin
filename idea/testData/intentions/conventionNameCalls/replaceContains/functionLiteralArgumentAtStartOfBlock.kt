// IGNORE_FE10_BINDING_BY_FIR
fun test() {
    class Test{
        operator fun contains(fn: () -> Boolean) : Boolean = true
    }
    val test = Test()
    if (true) {
        test.c<caret>ontains {
            true
        }
    }
}
