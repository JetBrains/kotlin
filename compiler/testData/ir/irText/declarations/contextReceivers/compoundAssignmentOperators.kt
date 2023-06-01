// !LANGUAGE: +ContextReceivers

data class Result(var i: Int)

var operationScore = 0

context(Int)
operator fun Result.plus(other: Result): Result {
    operationScore += this@Int
    return Result(i + other.i)
}

context(Int)
operator fun Result.plusAssign(other: Result) {
    operationScore += this@Int
    i += other.i
}

context(Int)
operator fun Result.minus(other: Result): Result {
    operationScore += this@Int
    return Result(i - other.i)
}

context(Int)
operator fun Result.minusAssign(other: Result) {
    operationScore += this@Int
    i -= other.i
}

context(Int)
operator fun Result.times(other: Result): Result {
    operationScore += this@Int
    return Result(i * other.i)
}

context(Int)
operator fun Result.timesAssign(other: Result) {
    operationScore += this@Int
    i *= other.i
}

context(Int)
operator fun Result.div(other: Result): Result {
    operationScore += this@Int
    return Result(i / other.i)
}

context(Int)
operator fun Result.divAssign(other: Result) {
    operationScore += this@Int
    i /= other.i
}

fun box(): String {
    val result = Result(0)
    with(1) {
        result += (Result(1) + Result(1))
        result -= (Result(1) - Result(0))
        result *= (Result(1) * Result(2))
        result /= (Result(4) / Result(2))
    }
    return if (result.i == 1 && operationScore == 8) "OK" else "fail"
}

