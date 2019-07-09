package localFunctionsWithReceivers

fun main() {
    fun String.foo(s: String) = this + 1
    fun String.foo() = this + 2

    val x = "a".foo("b")
    val y = "b".foo()
    //Breakpoint!
    val a = x + y
}

// EXPRESSION: "a".foo("b")
// RESULT: "a1": Ljava/lang/String;

// EXPRESSION: "b".foo()
// RESULT: "b2": Ljava/lang/String;