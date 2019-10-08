package delegatedVariables

fun main() {
    val a by lazy { "foo" }
    //Breakpoint!
    val b = a
}

// EXPRESSION: a
// RESULT: "foo": Ljava/lang/String;
