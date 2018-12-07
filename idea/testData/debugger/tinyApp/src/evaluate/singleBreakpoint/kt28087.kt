package kt28087

fun main() {
    "a".indexed { index, c ->
        //Breakpoint!
        val a = 5
    }
}

private inline fun CharSequence.indexed(action: (index: Int, Char) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

// EXPRESSION: index
// RESULT: 0: I