package initBlocks

class Foo {
    init {
        //Breakpoint!
        val a = 5
    }

    init {
        val b = 7
    }
}

fun main() {
    Foo()
}

// STEP_OVER: 5