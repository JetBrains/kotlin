// FIR_IDENTICAL

interface Continuation<in T>

abstract class C {
    abstract fun dispatchResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean
}
