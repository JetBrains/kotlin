package test

fun main() {
    val seq = sequence {
        //Breakpoint!
        yield(1)
        yield(2)
        yield(3)
    }

    seq.take(2).toList()
}

// STEP_OVER: 4