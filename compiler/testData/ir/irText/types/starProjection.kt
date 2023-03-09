// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

interface Continuation<in T>

abstract class C {
    abstract fun dispatchResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean
}
