import libN.*
import libO.*

suspend fun newMain() {
    newFoo()
    <warning descr="[DEPRECATION] 'oldFoo(): Unit' is deprecated. Experimental coroutines support will be dropped in 1.4">oldFoo</warning>()

    // TODO: actually, it's a bug
    oldMain()
}

fun newMain2() {
    newBuilder {
        newMain()
    }

    <error descr="[ILLEGAL_SUSPEND_FUNCTION_CALL] Suspend function 'oldFoo' should be called only from a coroutine or another suspend function">oldFoo</error>()
}
