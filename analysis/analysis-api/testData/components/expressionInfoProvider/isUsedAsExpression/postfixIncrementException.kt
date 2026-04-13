fun test(): Int {
    var result = 58
    <expr>result++</expr>
    return result
}

// FE1.0 `isUsedAsExpression` considers built-in postfix inc/dec and
// compound assignments as used, always.
