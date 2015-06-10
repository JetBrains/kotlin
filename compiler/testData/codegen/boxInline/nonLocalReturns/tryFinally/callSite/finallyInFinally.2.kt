package test

public inline fun doCall(block: ()-> Unit, finallyBlock1: ()-> Unit) {
    try {
         block()
    } finally {
        finallyBlock1()
    }
}