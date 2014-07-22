package test

public class Exception1(message: String) : java.lang.RuntimeException(message)

public class Exception2(message: String) : java.lang.RuntimeException(message)

public inline fun doCall(block: ()-> String, exception1: (e: Exception)-> Unit, exception2: (e: Exception)-> Unit, finallyBlock: ()-> String,
                         exception3: (e: Exception)-> Unit, exception4: (e: Exception)-> Unit, finallyBlock2: ()-> String, res: String = "Fail") : String {
    try {
        try {
            block()
        }
        catch (e: Exception1) {
            exception1(e)
        }
        catch (e: Exception2) {
            exception2(e)
        }
        finally {
            finallyBlock()
        }
    } catch (e: Exception1) {
        exception3(e)
    }
    catch (e: Exception2) {
        exception4(e)
    }
    finally {
        finallyBlock2()
    }
    return res
}