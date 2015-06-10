package test

public class Holder {
    public var value: String = ""
}

public inline fun <R> doCall2_2(block: () -> R, res: R, h: Holder): R {
    return doCall2_1(block, {
        h.value += ", OK_EXCEPTION"
        "OK_EXCEPTION"
    }, res, h)
}

public inline fun <R> doCall2_1(block: () -> R, exception: (e: Exception) -> Unit, res: R, h: Holder): R {
    return doCall2<R>(block, exception, {
        h.value += ", OK_FINALLY"
        "OK_FINALLY"
    }, res, h)
}

public inline fun <R> doCall2(block: () -> R, exception: (e: Exception) -> Unit, finallyBlock: () -> Unit, res: R, h: Holder): R {
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
        h.value += ", DO_CALL_2_FINALLY"
    }
    return res
}