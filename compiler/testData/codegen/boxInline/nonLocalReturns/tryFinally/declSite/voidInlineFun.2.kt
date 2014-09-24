package test

public inline fun <R> doCall(block: ()-> R, finallyBlock: ()-> Unit) {
    try {
        block()
    } finally {
        finallyBlock()
    }
}