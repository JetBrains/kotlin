// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// JVM_ABI_K1_K2_DIFF: KT-63864

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.Future

val count: Int = 10;
var index: Int = 0;
val doneSignal = CountDownLatch(count)
val startSignal = CountDownLatch(1);
val mutex: Any = Object()
val results = arrayListOf<Int>()
val executorService = Executors.newFixedThreadPool(count)

class MyException(message: String): Exception(message)

enum class ExecutionType {
    LOCAL,
    NON_LOCAL_SIMPLE,
    NON_LOCAL_EXCEPTION,
    NON_LOCAL_FINALLY,
    NON_LOCAL_EXCEPTION_AND_FINALLY,
    NON_LOCAL_EXCEPTION_AND_FINALLY_WITH_RETURN,
    NON_LOCAL_NESTED
}

class TestLocal(val name: String, val executionType: ExecutionType) : Callable<String> {

    override fun call(): String {
        startSignal.await()
        return when (executionType) {
            ExecutionType.LOCAL -> local()
            ExecutionType.NON_LOCAL_SIMPLE -> nonLocalSimple()
            ExecutionType.NON_LOCAL_EXCEPTION -> nonLocalWithException()
            ExecutionType.NON_LOCAL_FINALLY -> nonLocalWithFinally()
            ExecutionType.NON_LOCAL_EXCEPTION_AND_FINALLY -> nonLocalWithExceptionAndFinally()
            ExecutionType.NON_LOCAL_EXCEPTION_AND_FINALLY_WITH_RETURN -> nonLocalWithExceptionAndFinallyWithReturn()
            ExecutionType.NON_LOCAL_NESTED -> nonLocalNested()
            else -> "fail"
        }
    }

    private fun underMutexFun() {
        results.add(++index);
        doneSignal.countDown()
    }

    fun local(): String {
        synchronized(mutex) {
            underMutexFun()
        }
        return executionType.toString()
    }


    fun nonLocalSimple(): String {
        synchronized(mutex) {
            underMutexFun()
            return executionType.name
        }
        return "fail"
    }

    fun nonLocalWithException(): String {
        synchronized(mutex) {
            try {
                underMutexFun()
                throw MyException(executionType.name)
            } catch (e: MyException) {
                return e.message!!
            }
        }
        return "fail"
    }

    fun nonLocalWithFinally(): String {
        synchronized(mutex) {
            try {
                underMutexFun()
                return "fail"
            } finally {
                return executionType.name
            }
        }
        return "fail"
    }

    fun nonLocalWithExceptionAndFinally(): String {
        synchronized(mutex) {
            try {
                underMutexFun()
                throw MyException(executionType.name)
            } catch (e: MyException) {
                return e.message!!
            } finally {
                "123"
            }
        }
        return "fail"
    }

    fun nonLocalWithExceptionAndFinallyWithReturn(): String {
        synchronized(mutex) {
            try {
                underMutexFun()
                throw MyException(executionType.name)
            } catch (e: MyException) {
                return "fail1"
            } finally {
                return executionType.name
            }
        }
        return "fail"
    }

    fun nonLocalNested(): String {
        synchronized(mutex) {
            try {
                try {
                    underMutexFun()
                    throw MyException(executionType.name)
                } catch (e: MyException) {
                    return "fail1"
                } finally {
                    return executionType.name
                }
            } finally {
                val p = 1 + 1
            }
        }
        return "fail"
    }
}

fun testTemplate(type: ExecutionType, producer: (Int) -> Callable<String>): String {

    try {
        val futures = arrayListOf<Future<String>>()
        for (i in 1..count) {
            futures.add(executorService.submit (producer(i)))
        }

        startSignal.countDown()
        val b = doneSignal.await(10, TimeUnit.SECONDS)
        if (!b) return "fail: processes not finished"

        for (i in 1..count) {
            if (results[i - 1] != i)
                return "fail $i != ${results[i]}: synchronization not works : " + results.joinToString()
        }

        for (f in futures) {
            if (f.get() != type.name) return "failed result ${f.get()} != ${type.name}"
        }
    } finally {

    }

    return "OK"
}

fun runTest(type: ExecutionType): String {
    return testTemplate (type) { TestLocal(it.toString(), type) }
}

fun box(): String {
    try {
        for (type in ExecutionType.values()) {
            val result = runTest(type)
            if (result != "OK") return "fail on $type execution: $result"
        }
    } finally {
        executorService.shutdown()
    }
    return "OK"
}
