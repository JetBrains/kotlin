package frameLambdaVariableFromCorrectFrame

fun main() {
    val k = "lorem"
    foo {
        val j = "ipsum"
        {
            val l = 5
            //Breakpoint!
            Unit
        }()
    }
}

fun foo(block: () -> Any?) {
    val k = "doloret"
    val j = "sit"
    block()
}

// PRINT_FRAME

// EXPRESSION: k
// RESULT: "lorem": Ljava/lang/String;

// EXPRESSION: j
// RESULT: "ipsum": Ljava/lang/String;