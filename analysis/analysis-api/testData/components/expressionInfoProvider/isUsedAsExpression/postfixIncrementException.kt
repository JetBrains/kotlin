fun test(): Int {
    var result = 58
    <expr>result++</expr>
    return result
}

// IGNORE_FE10
// FE1.0 `isUsedAsExpression` considers built-in postfix inc/dec and
// compound assignments as used, always.