// COMPILATION_ERRORS

val expressionBody: Int get() = 1

val lambdaBody get() = {}

val blockBody: Int get() { return 1 }

var setterExpressionBody: Int = 0
    set(value) = Unit

val noBody: Int get

val expressionWithoutEq: Int get() 1

val blockThenEq: Int get() {} = 1

val lambdaThenEq get() = {} = 1

val doubleEq: Int get() = = 1

val missingExpressionBeforeSemicolon: Int get() = ;

val missingExpression: Int get() =
