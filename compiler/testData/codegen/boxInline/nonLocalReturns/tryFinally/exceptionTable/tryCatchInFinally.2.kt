package test

public class Holder(var value: String = "") {

    public fun plusAssign(s: String?) {
        if (value.length != 0) {
            value += " -> "
        }
        value += s
    }

    override fun toString(): String {
        return value
    }

}

public class Exception1(message: String) : java.lang.RuntimeException(message)

public class Exception2(message: String) : java.lang.RuntimeException(message)

public inline fun doCall(block: ()-> String, finallyBlock: ()-> String,
                         tryBlock2: ()-> String, catchBlock2: ()-> String, res: String = "Fail") : String {
    try {
        block()
    }
    finally {
        finallyBlock()
        try {
            tryBlock2()
        } catch (e: Exception) {
            catchBlock2()
        }
    }
    return res
}