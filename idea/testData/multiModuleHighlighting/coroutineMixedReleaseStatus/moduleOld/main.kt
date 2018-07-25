import libN.*
import libO.*

suspend fun oldMain() {
    <error descr="[VERSION_REQUIREMENT_DEPRECATION_ERROR] 'newFoo(): Unit' is only available since Kotlin 1.3 and cannot be used in Kotlin 1.2">newFoo</error>()
    oldFoo()
}

fun oldMain2() {
    <error descr="[VERSION_REQUIREMENT_DEPRECATION_ERROR] 'newBuilder((Continuation<Unit>) -> Any?): Unit' is only available since Kotlin 1.3 and cannot be used in Kotlin 1.2">newBuilder</error> {
        <error descr="[ILLEGAL_SUSPEND_FUNCTION_CALL] Suspend function 'oldMain' should be called only from a coroutine or another suspend function">oldMain</error>()
    }

    oldBuilder {
        oldMain()
    }
}
