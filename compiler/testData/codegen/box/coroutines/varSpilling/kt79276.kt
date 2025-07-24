// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class LoginViewModel {
    suspend fun testSuspend(): Unit = println("test")

    fun onSubmit2(): Unit {
        runBlocking {
            1F.also { println(it) }
            try {
                testSuspend()
            } catch (throwable: Throwable) {
                testSuspend()
                throw throwable
            } finally {
                1.also { println(it) }
            }
        }
    }
}

fun box(): String {
    LoginViewModel().onSubmit2()
    return "OK"
}