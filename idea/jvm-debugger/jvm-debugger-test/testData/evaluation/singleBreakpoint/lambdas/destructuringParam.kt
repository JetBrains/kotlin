package destructuringParam
data class A(val x: String, val y: String)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {

}

fun main(args: Array<String>) {
    //Breakpoint! (lambdaOrdinal = 1)
    foo(A("O", "K")) { (x, y) -> x + y }
}

// PRINT_FRAME

// EXPRESSION: x
// RESULT: "O": Ljava/lang/String;

// EXPRESSION: y
// RESULT: "K": Ljava/lang/String;
