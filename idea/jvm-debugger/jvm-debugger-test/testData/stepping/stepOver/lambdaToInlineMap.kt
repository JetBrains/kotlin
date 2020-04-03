package test

fun main() {
    val l = listOf(1, 2, 3, 4)
    l.map { element ->
        //Breakpoint!
        bar(element * 2)
    }
}

fun bar(n: Int) = n

// STEP_OVER: 5