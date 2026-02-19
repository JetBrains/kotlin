// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

data class Result(val i: Int)

var operationScore = 0

context(_context0: Int)
operator fun Result.unaryMinus(): Result {
    operationScore += _context0
    return Result(-i)
}

context(_context0: Int)
operator fun Result.unaryPlus(): Result {
    operationScore += _context0
    return Result(if (i < 0) (-i) else i)
}

context(_context0: Int)
operator fun Result.inc(): Result {
    operationScore += _context0
    return Result(i + 1)
}

context(_context0: Int)
operator fun Result.dec(): Result {
    operationScore += _context0
    return Result(i - 1)
}

fun box(): String {
    var result = Result(0)
    with(1) {
        result++
        result++
        (-result)
        +result
        result--
    }
    return if (result.i == 1 && operationScore == 5) "OK" else "fail"
}
