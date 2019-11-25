// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

import java.io.Closeable

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
    }
}

fun box(): String {
    var callable = TestLocal()
    var result = callable.local()
    if (result.value != "called->closed") return "fail local: " + result.value

    callable = TestLocal()
    result = callable.nonLocalSimple()
    if (result.value != "called->closed") return "fail nonLocalSimple: " + result.value

    callable = TestLocal()
    result = callable.nonLocalWithException()
    if (result.value != "called->exception->closed") return "fail nonLocalWithException: " + result.value

    callable = TestLocal()
    result = callable.nonLocalWithFinally()
    if (result.value != "called->finally->closed") return "fail nonLocalWithFinally: " + result.value

    callable = TestLocal()
    result = callable.nonLocalWithExceptionAndFinally()
    if (result.value != "called->exception->finally->closed") return "fail nonLocalWithExceptionAndFinally: " + result.value

    callable = TestLocal()
    result = callable.nonLocalWithExceptionAndFinallyWithReturn()
    if (result.value != "called->exception->finally->closed") return "fail nonLocalWithExceptionAndFinallyWithReturn: " + result.value

    callable = TestLocal()
    result = callable.nonLocalNestedWithException()
    if (result.value != "called->exception->finally1->finally2->closed") return "fail nonLocalNestedWithException: " + result.value

    callable = TestLocal()
    result = callable.nonLocalNestedFinally()
    if (result.value != "called->finally1->finally2->closed") return "fail nonLocalNestedFinally: " + result.value


    return "OK"
}
