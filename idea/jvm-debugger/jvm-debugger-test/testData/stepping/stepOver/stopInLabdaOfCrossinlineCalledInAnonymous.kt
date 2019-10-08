package stopInLabdaOfCrossinlineCalledInAnonymous

// KT-12612

class C {}
interface D {
    fun run()
}

inline fun C.bar(crossinline lambda: () -> Unit) {
    object: D {
        override fun run() {
            lambda()
        }
    }.run()
}

fun foo(c: C) {
    c.bar {
        //Breakpoint!
        nop()
        nop()
    }
}

fun main(args: Array<String>) {
    foo(C())
}

fun nop() {}