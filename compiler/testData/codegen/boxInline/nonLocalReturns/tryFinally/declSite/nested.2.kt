package test

public inline fun doCall(block: ()-> Unit, finallyBlock1: ()-> Unit, finallyBlock2: ()-> Unit) {
    try {
        try {
            block()
        }
        finally {
            finallyBlock1()
        }
    } finally {
        finallyBlock2()
    }
}