package frameThis0This0

fun main(args: Array<String>) {
    A().test()
}

class A {
    val prop1 = 1

    fun test() {
        val val1 = 1
        foo {
            val val2 = 1
            foo {
                //Breakpoint!
                prop1 + val1 + val2
            }
        }
    }
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME