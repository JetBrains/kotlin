package frameThis0

fun main(args: Array<String>) {
    A().test()
}

class A {
    val prop1 = 1

    fun test() {
        val val1 = 1
        foo {
            val val2 = 1
            //Breakpoint!
            prop1 + val1 + val2
        }
    }
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME