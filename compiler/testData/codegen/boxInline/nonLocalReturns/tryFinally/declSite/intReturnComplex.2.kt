package test

public class Holder {
    public var value: String = ""
}

public inline fun doCall(block: ()-> Int, exception: (e: Exception)-> Unit, finallyBlock: ()-> Int, h : Holder, res: Int = -111) : Int {
    try {
        try {
            return block()
        }
        catch (e: Exception) {
            exception(e)
        }
        finally {
            finallyBlock()
        }
    } finally {
        h.value += ", INLINE_CALL_FINALLY"
    }
    return res
}

public inline fun <R> doCall2(block: ()-> R, exception: (e: Exception)-> Unit, finallyBlock: ()-> R, res: R, h : Holder) : R {
    try {
        try {
            return block()
        }
        catch (e: Exception) {
            exception(e)
        }
        finally {
            finallyBlock()
        }
    } finally {
        h.value += ", INLINE_CALL_FINALLY"
    }
    return res
}