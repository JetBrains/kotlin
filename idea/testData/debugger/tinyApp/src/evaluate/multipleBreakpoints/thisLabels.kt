package thisLabels

fun main() {
    block(1) a@ {
        //Breakpoint!
        inlineBlock(2) b@ {
            //Breakpoint!
            block(3) c@ {
                //Breakpoint!
                val a = 5
            }
        }
    }
}

fun <T> block(t: T, block: T.() -> Unit) {
    t.block()
}

fun <T> T.inlineBlock(t: T, block: T.() -> Unit) {
    t.block()
}

// EXPRESSION: this
// RESULT: 1: I

// EXPRESSION: this
// RESULT: 2: I

// EXPRESSION: this@c
// RESULT: 3: I