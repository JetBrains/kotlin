package test

public class Exception1(message: String) : java.lang.RuntimeException(message)

public class Exception2(message: String) : java.lang.RuntimeException(message)

public inline fun doCall(block: ()-> String, finallyBlock: ()-> String,
                         finallyBlock2: ()-> String, res: String = "Fail") : String {
    try {
        try {
            block()
        }
        finally {
            if (true) {
                finallyBlock()
                /*External finally would be injected here*/
                return res + "_INNER_FINALLY"
            }
        }
    } finally {
        finallyBlock2()
    }
    return res
}