package test

public inline fun <R> doCall(block: ()-> R, finallyLambda: ()-> Unit) : R {
    try {
        return block()
    } finally {
        finallyLambda()
    }
}