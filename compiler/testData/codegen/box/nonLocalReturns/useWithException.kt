// TARGET_BACKEND: JVM

// WITH_RUNTIME

import java.io.Closeable
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.test.assertEquals

class MyException(message: String) : Exception(message)

class Holder(var value: String) {
    operator fun plusAssign(s: String?) {
        value += s
        if (s != "closed") {
            value += "->"
        }
    }
}

class TestLocal() : Closeable {

    var status = Holder("")

    private fun underMutexFun() {
        status += "called"
    }

    fun local(): Holder {
        use {
            underMutexFun()
        }
        return status
    }


    fun nonLocalSimple(): Holder {
        use {
            underMutexFun()
            return status
        }
        return Holder("fail")
    }

    fun nonLocalWithException(): Holder {
        use {
            try {
                underMutexFun()
                throw MyException("exception")
            } catch (e: MyException) {
                status += e.message!!
                return status
            }
        }
        return Holder("fail")
    }

    fun nonLocalWithFinally(): Holder {
        use {
            try {
                underMutexFun()
                return Holder("fail")
            } finally {
                status += "finally"
                return status
            }
        }
        return Holder("fail")
    }

    fun nonLocalWithExceptionAndFinally(): Holder {
        use {
            try {
                underMutexFun()
                throw MyException("exception")
            } catch (e: MyException) {
                status += e.message
                return status
            } finally {
                status += "finally"
            }
        }
        return Holder("fail")
    }

    fun nonLocalWithExceptionAndFinallyWithReturn(): Holder {
        use {
            try {
                underMutexFun()
                throw MyException("exception")
            } catch (e: MyException) {
                status += e.message
                return Holder("fail")
            } finally {
                status += "finally"
                return status
            }
        }
        return Holder("fail")
    }

    fun nonLocalNestedWithException(): Holder {
        use {
            try {
                try {
                    underMutexFun()
                    throw MyException("exception")
                } catch (e: MyException) {
                    status += "exception"
                    return Holder("fail")
                } finally {
                    status += "finally1"
                    return status
                }
            } finally {
                status += "finally2"
            }
        }
        return Holder("fail")
    }

    fun nonLocalNestedFinally(): Holder {
        use {
            try {
                try {
                    underMutexFun()
                    return status
                } finally {
                    status += "finally1"
                    status
                }
            } finally {
                status += "finally2"
            }
        }
        return Holder("fail")
    }

    override fun close() {
        status += "closed"
        throw MyException("error")
    }
}

fun box(): String {
    assertError(1,"called->closed") {
        local()
    }

    assertError(2, "called->closed") {
        nonLocalSimple()
    }

    assertError(3, "called->exception->closed") {
        nonLocalWithException()
    }

    assertError(4, "called->finally->closed") {
        nonLocalWithFinally()
    }

    assertError(5, "called->exception->finally->closed") {
        nonLocalWithExceptionAndFinally()
    }

    assertError(6, "called->exception->finally->closed") {
        nonLocalWithExceptionAndFinallyWithReturn()
    }

    assertError(7, "called->exception->finally1->finally2->closed") {
        nonLocalNestedWithException()
    }

    assertError(8, "called->finally1->finally2->closed") {
        nonLocalNestedFinally()
    }

    return "OK"
}

public fun assertError(index: Int, expected: String, l: TestLocal.()->Unit) {
    val testLocal = TestLocal()
    try {
        testLocal.l()
        fail("fail $index: no error")
    } catch (e: Exception) {
        assertEquals(expected, testLocal.status.value, "failed on $index")
    }
}
