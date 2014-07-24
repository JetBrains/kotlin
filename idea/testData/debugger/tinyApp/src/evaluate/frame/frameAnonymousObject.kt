package frameAnonymousObject

fun main(args: Array<String>) {
    val val1 = 1
    val o = object {
        val obProp = 1

        fun run() {
            foo {
                //Breakpoint!
                val1 + obProp
            }
        }
    }

    o.run()
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME