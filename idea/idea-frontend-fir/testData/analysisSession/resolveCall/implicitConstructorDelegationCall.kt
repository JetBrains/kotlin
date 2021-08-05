class A {
    constructor(i: Int)
}

fun call() {
    val a = <expr>A(42)</expr>
}

// CALL: KtFunctionCall: targetFunction = <constructor>(i: kotlin.Int): A
