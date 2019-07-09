package kt15259

interface ObjectFace

private fun makeFace() = object : ObjectFace {
//Breakpoint!
}

fun main() {
    makeFace()
}

// EXPRESSION: this
// RESULT: 'this' is not defined in this context