fun test(): Boolean {
    var n = 456
    <expr>n %= 45</expr>
    return n < 45
}

// IGNORE_FE10
// FE1.0 `isUsedAsExpression` considers built-in postfix inc/dec and
// compound assignments as used, always.