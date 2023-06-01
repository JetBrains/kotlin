// !LANGUAGE: +ContextReceivers

data class Result(val i: Int)

var operationScore = 0

context(Int)
operator fun Result.unaryMinus(): Result {
    operationScore += this@Int
    return Result(-i)
}

context(Int)
operator fun Result.unaryPlus(): Result {
    operationScore += this@Int
    return Result(if (i < 0) (-i) else i)
}

context(Int)
operator fun Result.inc(): Result {
    operationScore += this@Int
    return Result(i + 1)
}

context(Int)
operator fun Result.dec(): Result {
    operationScore += this@Int
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


