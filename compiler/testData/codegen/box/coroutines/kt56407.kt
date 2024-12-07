// WITH_STDLIB
// CHECK_BYTECODE_LISTING___ // Temporary turn off the directive, revert after the next bootstrapt advance
// IGNORE_INLINER___: IR // Temporary turn off the directive, revert after the next bootstrapt advance

import kotlin.coroutines.*

interface ServerBase

interface Client<ServerType : ServerBase> {
    suspend fun connectToServer()
}

public suspend inline fun <E> E.consumeEach(action: (E) -> Unit): Unit = action(this)

fun builder(c: suspend (String) -> Unit) {
    c.startCoroutine("", Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

abstract class DefaultAuthorizableClient<ServerType : ServerBase> : Client<ServerType> {
    override suspend fun connectToServer() {
        class NextObjectQuery

        val nextObjectQuery = NextObjectQuery()

        builder {
            consumeEach { nextObjectQuery }
        }

    }
}

fun box(): String {
    builder {
        // make sure all classfiles are loaded
        object : DefaultAuthorizableClient<ServerBase>() {}.connectToServer()
    }

    return "OK"
}