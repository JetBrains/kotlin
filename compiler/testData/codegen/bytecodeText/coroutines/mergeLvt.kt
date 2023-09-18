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

// JVM_TEMPLATES
// 1 LOCALVARIABLE \$this\$extensionFun\$iv\$iv LAtomicInt;

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE \$this\$extensionFun\$iv\$iv LAtomicInt;

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 1 LOCALVARIABLE \$this\$extensionFun\\2 LAtomicInt;
