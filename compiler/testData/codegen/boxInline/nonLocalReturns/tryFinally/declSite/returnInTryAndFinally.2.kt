package test

public inline fun <R> doCall(block: ()-> R, finallyBlock: ()-> R) : R {
    try {
        return block()
    } finally {
        return finallyBlock()
    }
}