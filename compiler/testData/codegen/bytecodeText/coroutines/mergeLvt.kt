import kotlin.coroutines.intrinsics.*

class AtomicInt(val value: Int)

fun atomic(i: Int) = AtomicInt(i)

class MyBlockingAdapter() {
    private val state = atomic(0)
    private val a = 77
    suspend fun foo() {
        val a = suspendBar()
    }
    private inline fun AtomicInt.extensionFun() {
        if (a == 77) throw IllegalStateException("AAAAAAAAAAAA")
        value
    }
    private suspend inline fun suspendBar() {
        state.extensionFun()
        suspendCoroutineUninterceptedOrReturn<Any?> { ucont ->
            COROUTINE_SUSPENDED
        }
    }
}

// 1 LOCALVARIABLE \$this\$extensionFun\$iv\$iv LAtomicInt;
