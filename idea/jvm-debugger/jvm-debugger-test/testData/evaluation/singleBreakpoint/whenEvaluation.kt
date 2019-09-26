package whenEvaluation

fun main(args: Array<String>) {
    val a = "x"
    //Breakpoint!
    args.size
}

// EXPRESSION: when (a) { "a" -> "A"; "b" -> "B"; else -> "C" }
// RESULT: "C": Ljava/lang/String;