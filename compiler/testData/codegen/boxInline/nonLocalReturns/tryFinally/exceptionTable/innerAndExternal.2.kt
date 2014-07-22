package test

public class Exception1(message: String) : java.lang.RuntimeException(message)

public class Exception2(message: String) : java.lang.RuntimeException(message)

public inline fun doCall(block: ()-> String, exception: (e: Exception)-> Unit, exception2: (e: Exception)-> Unit, finallyBlock: ()-> String, res: String = "Fail") : String {
    try {
        block()
    } catch (e: Exception1) {
        exception(e)
    } catch (e: Exception2) {
        exception2(e)
    } finally {
        finallyBlock()
    }
    return res
}

public inline fun <R> doCall2(block: ()-> R, exception: (e: Exception)-> Unit, finallyBlock: ()-> R) : R {
    try {
        return block()
    } catch (e: Exception) {
        exception(e)
    } finally {
        finallyBlock()
    }
    throw java.lang.RuntimeException("fail")
}