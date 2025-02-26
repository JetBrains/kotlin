private val unusedBefore1 = { 1 }
private val unusedBefore2 = { "unused" }

inline fun calculate(): Int {
    val lambda = {
        40 + 2
    }
    return lambda()
}

private val unusedAfter1 = { true }
private val unusedAfter2 = { 42.0 }
