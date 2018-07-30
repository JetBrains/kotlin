import libN.*
import libO.*

suspend fun newMain() {
    newFoo()
    <error descr="[DEPRECATION_ERROR] Using 'oldFoo(): Unit' is an error. Experimental coroutine cannot be used with API version 1.3">oldFoo</error>()

    // TODO: actually, it's a bug
    oldMain()
}

fun newMain2() {
    newBuilder {
        newMain()
    }

    <error descr="[DEPRECATION_ERROR] Using 'oldFoo(): Unit' is an error. Experimental coroutine cannot be used with API version 1.3"><error descr="[ILLEGAL_SUSPEND_FUNCTION_CALL] Suspend function 'oldFoo' should be called only from a coroutine or another suspend function">oldFoo</error></error>()
}
