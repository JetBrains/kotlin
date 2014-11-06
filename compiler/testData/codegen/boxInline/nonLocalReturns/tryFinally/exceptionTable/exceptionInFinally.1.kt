import test.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class MyException(message: String) : Exception(message)

class Holder(var value: String) {
    public fun plusAssign(s: String?) {
        value += s
        if (s != "closed") {
            value += "->"
        }
    }
}

class Test() : MCloseable {

    val status = Holder("")

    private fun jobFun() {
        status += "called"
    }

    fun nonLocalWithExceptionAndFinally(): Holder {
        muse {
            try {
                jobFun()
                throw MyException("exception")
            }
            catch (e: MyException) {
                status += e.getMessage()
                return status
            }
            finally {
                status += "finally"
            }
        }
        return Holder("fail")
    }

    override fun close() {
        status += "closed"
        throw MyException("error")
    }
}

fun box() : String {
    assertError(1, "called->exception->finally->closed") {
        nonLocalWithExceptionAndFinally()
    }

    return "OK"
}

public fun assertError(index: Int, expected: String, l: Test.()->Unit) {
    val testLocal = Test()
    try {
        testLocal.l()
        fail("fail $index: no error")
    } catch (e: Exception) {
        assertEquals(expected, testLocal.status.value, "failed on $index")
    }
}
