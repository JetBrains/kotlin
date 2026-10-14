fun compute(): Int = 0

fun evaluate(block: () -> Int): Int = block()

fun combine(value: Int, block: () -> Int): Int = value + block()

val trailingLambda: Int = evaluate { 1 }

val positionalLambdaArgument: Int = evaluate({ 2 })

val namedLambdaArgument: Int = evaluate(block = { 3 })

val mixedArguments: Int = combine(4, { 5 })

val parenthesizedLambdaArgument: Int = evaluate(({ 9 }))

val conditional: Int = if (compute() == 0) 6 else 7

val objectLiteral: Any = object : Any() {}

val callableReference: () -> Int = ::compute

val interpolation: String = "${compute()}"

fun expressionBodyWithLambda(): Int = evaluate { 8 }
