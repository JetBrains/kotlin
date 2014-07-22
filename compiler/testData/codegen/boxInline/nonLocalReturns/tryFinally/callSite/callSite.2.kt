package test

public inline fun <R> doCall(block: ()-> R) : R {
    return block()
}