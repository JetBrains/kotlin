// PROBLEM: none
// WITH_RUNTIME

fun foo() {
    val xs = listOf(1, 2, 3).flatMap { x ->
        listOf(3, 4, 5).map { y ->
            object {
                val <caret>value = x + y
            }
        }
    }

    xs.forEach {
        println("value: " + it.value)
    }
}