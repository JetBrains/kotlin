fun test(a: Any) {
    // Type checks (KtIsExpression)
    a is String
    a !is String

    // Type casts (KtBinaryExpressionWithTypeRHS)
    a as String
    a as? String
}
