// RUN_PIPELINE_TILL: BACKEND
import kotlin.reflect.KSuspendFunction1

fun <T> runBlocking(block: suspend () -> T): T = null!!

// test 1
object Foo {
    suspend fun bar() {
    }
}
fun test1() {
    runBlocking {
        val someLambda: () -> suspend() -> Unit = {
            Foo::<!NON_LOCAL_SUSPENSION_POINT!>bar<!>
        }

        someLambda()
    }
}

// test 2
fun createBlock(block: () -> Unit) {
    block()
}
fun receiver(block: suspend (context: Any) -> Unit) {
}
suspend fun mySuspend(arg: Any) {
}
suspend fun test2() {
    createBlock {
        receiver(::<!NON_LOCAL_SUSPENSION_POINT!>mySuspend<!>)
    }
}

// test 3
suspend fun fun1(arg: String) = Unit

suspend fun <A, B> takeRef(supplier: suspend () -> KSuspendFunction1<A, B>) = Unit

fun test3() = runBlocking {
    takeRef { ::fun1.apply { } }
    takeRef { ::<!NON_LOCAL_SUSPENSION_POINT!>fun1<!> }
}