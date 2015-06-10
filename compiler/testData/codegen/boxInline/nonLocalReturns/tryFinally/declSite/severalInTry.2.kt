package test

public inline fun doCall(block: ()-> Unit, block2: ()-> Unit, finallyBlock2: ()-> Unit) {
    try {
         block()
         block2()
    } finally {
        finallyBlock2()
    }
}