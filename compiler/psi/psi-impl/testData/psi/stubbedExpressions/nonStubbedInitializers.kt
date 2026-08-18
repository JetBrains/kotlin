// The stub tree stops at an expression that has no stub-based counterpart,
// and nothing below such an expression is stubbed either
package test

fun compute(): Int = 0

fun evaluate(block: () -> Int): Int = block()

fun combine(value: Int, block: () -> Int): Int = value + block()

val lambdaArgument: Int = evaluate { 1 }

val namedLambdaArgument: Int = evaluate(block = { 2 })

val positionalLambdaArgument: Int = evaluate({ 10 })

// The first argument is stub-based and the second one is not, so the argument list is only partially represented
val mixedArguments: Int = combine(11, { 12 })

// The parenthesized expression is stub-based while the lambda inside it is not
val parenthesizedLambdaArgument: Int = evaluate(({ 13 }))

val lambda: () -> Int = { 3 }

val conditional: Int = if (compute() == 0) 4 else 5

val whenExpression: Int = when (compute()) {
    0 -> 6
    else -> 7
}

val objectLiteral: Any = object : Any() {}

val callableReference: () -> Int = ::compute

val interpolation: String = "${compute()}"

val localDeclaration: Int = evaluate {
    val local = 8
    local
}

fun expressionBodyWithLambda(): Int = evaluate { 9 }
