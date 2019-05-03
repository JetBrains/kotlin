package lambdaThisMangling

private fun block(block: (String) -> Unit) {
    block("foo")
}

fun main() {
    block { foo ->
        //Breakpoint!
        val a = 5
    }
}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME

// EXPRESSION: foo
// RESULT: "foo": Ljava/lang/String;