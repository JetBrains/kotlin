package test

public class Exception1(message: String) : java.lang.RuntimeException(message)

public inline fun doCall(block: ()-> String, exception: (e: Exception)-> Unit, finallyBlock: ()-> String, res: String = "Fail") : String {
    try {
        block()
    } catch (e: Exception1) {
        exception(e)
    } finally {
        finallyBlock()
    }
    return res
}
