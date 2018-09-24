// PROBLEM: none

enum class Test(f: () -> Unit) {
    A(<caret>getFunc())
}

fun getFunc(): () -> Unit = {}