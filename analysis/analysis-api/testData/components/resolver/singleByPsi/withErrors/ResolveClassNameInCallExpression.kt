class Test<T>

// The lack of `()` in the constructor call is intentional. The addition of type arguments is also needed so we have a KtCallExpression.
// We want to test finding the symbol for a FirResolvedQualifier (and not a FirFunctionCall) whose source is a KtCallExpression
// (and not a KtSimpleNameExpression).
val t = <caret>Test<Int>
