package stopInObjectLiteralInInlineCallNoClosure

// KT-12734

fun main(args: Array<String>) {
    val a = 12

    inlineF {
        val s = object: () -> Unit {
            override fun invoke() {
                //Breakpoint!
                nop()
                nop()
            }
        }

        s()
    }
}

inline fun <R> inlineF(block: () -> R): R = block()

fun nop() {}