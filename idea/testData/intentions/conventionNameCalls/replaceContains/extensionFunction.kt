// IGNORE_FE10_BINDING_BY_FIR
fun test() {
    class Test()
    operator fun Test.contains(a: Int) : Boolean = true
    val test = Test()
    test.c<caret>ontains(1)
}
