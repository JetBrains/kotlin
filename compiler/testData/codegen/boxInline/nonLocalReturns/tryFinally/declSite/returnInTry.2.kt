package test

public inline fun <R> doCall(block: ()-> R, finallyBlock: ()-> Unit) : R {
    try {
        return block()
    } finally {
        finallyBlock()
    }
}