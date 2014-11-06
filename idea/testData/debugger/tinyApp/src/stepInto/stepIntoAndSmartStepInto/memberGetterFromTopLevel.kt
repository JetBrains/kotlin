package memberGetterFromTopLevel

class A {
    val bar: Int
        get() {
            val a = 1
            return 1
        }
}

fun main(args: Array<String>) {
    A()

    //Breakpoint!
    A().bar
}
