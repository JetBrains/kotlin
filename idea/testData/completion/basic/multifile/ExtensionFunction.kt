package first

class FirstClass() {
}

fun firstFun() {
    val a = FirstClass()
    a.<caret>
}

// INVOCATION_COUNT: 2
// EXIST: secondExtension