// COMPILATION_ERRORS

fun expressionBody() = 1

fun lambdaBody() = {}

fun blockBody() {}

fun noBody()

fun <T> whereClause() where T : Any = 1

fun contract() contract [returns()] = 1

fun expressionWithoutEq() 1

fun blockThenEq() {} = 1

fun lambdaThenEq() = {} = 1

fun doubleEq() = = 1

fun missingExpressionBeforeSemicolon() = ;

{}

fun missingExpression() =
