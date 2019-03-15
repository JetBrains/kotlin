@file:Suppress("UNUSED_PARAMETER")

import kotlin.coroutines.*
import org.jetbrains.annotations.BlockingContext
import java.lang.Thread.sleep

suspend fun testFunction() {
    @BlockingContext
    val ctx = getContext()
    // no warnings with @BlockingContext annotation on ctx object
    withContext(ctx) {Thread.sleep (2)}

    // no warnings with @BlockingContext annotation on getContext() method
    withContext(getContext()) {Thread.sleep(3)}

    withContext(getNonBlockingContext()) {
        Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(3);
    }
}

@BlockingContext
fun getContext(): CoroutineContext = TODO()

fun getNonBlockingContext(): CoroutineContext = TODO()

suspend fun <T> withContext(
    context: CoroutineContext,
    f: suspend () -> T
) {
    TODO()
}