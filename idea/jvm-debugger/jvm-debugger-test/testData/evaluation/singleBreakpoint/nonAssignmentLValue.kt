package foo

fun main() {
    val existing = mutableListOf<Int>()
    for (i in 1..10) {
        //Breakpoint!
        if (existing.none { i % it != 0 }) {
            existing += i
        }
    }
}

// EXPRESSION: existing.none { i % it != 0 }
// RESULT: 1: Z