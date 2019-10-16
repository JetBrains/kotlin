package inlineCallInForRangeExpression

fun <T> nonInline(p: T) = p

fun main(args: Array<String>) {
    //Breakpoint!
    nonInline(12)
    for (e in range()) {
        nonInline(e)
    }
}

inline fun range() = 1..2

// STEP_OVER: 6