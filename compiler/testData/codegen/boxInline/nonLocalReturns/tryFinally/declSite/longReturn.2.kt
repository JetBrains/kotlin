package test

public inline fun doCall(block: ()-> Long, exception: (e: Exception)-> Unit, finallyBlock: ()-> Long, res: Long = -1111.toLong()) : Long {
    try {
        block()
    } catch (e: Exception) {
        exception(e)
    } finally {
        finallyBlock()
    }
    return res
}

public inline fun <R> doCall2(block: ()-> R, exception: (e: Exception)-> Unit, finallyBlock: ()-> R, res: R) : R {
    try {
        return block()
    } catch (e: Exception) {
        exception(e)
    } finally {
        finallyBlock()
    }
    return res
}