package test

public inline fun <R> doCall(block: ()-> R, finallyBlock: ()-> R) : R {
    try {
        block()
    } finally {
        return finallyBlock()
    }
}