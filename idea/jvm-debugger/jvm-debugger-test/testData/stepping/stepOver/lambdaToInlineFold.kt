package test

fun main() {
    val l = listOf(1, 2, 3, 4)
    val res = l.foldRightIndexed(0) { index, elem, akk ->
        //Breakpoint!
        akk + index - 4
    }
}

// STEP_OVER: 5