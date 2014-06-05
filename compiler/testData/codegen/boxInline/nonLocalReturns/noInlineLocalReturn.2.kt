package test

public fun <R> noInlineCall(block: ()-> R) : R {
    return block()
}

public inline fun <R> notUsed(block: ()-> R) : R {
    return block()
}