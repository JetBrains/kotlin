package test

class SuspendLambda {
    fun <T> (suspend () -> T).createCoroutine1(completion: Continuation<T>) {}

    fun <R, T> (suspend R.() -> T).createCoroutine2(receiver: R, completion: Continuation<T>) {}

    fun <R, T> (suspend @receiver:A R.() -> T).createCoroutineAnother() {}

    fun <T> testCoroutine(f: suspend (Int) -> T?) {}

    fun <T> testCoroutineWithAnnotation(f: suspend (Int) -> @A T?) {}

    var nullableSuspend: (suspend (P) -> Unit)? = null
    var nullableSuspendWithReceiver: (suspend RS.(P) -> Unit)? = null
    var nullableSuspendWithNullableParameter: (suspend (P?) -> Unit)? = null
    var nullableSuspendWithNullableReceiver: (suspend RS?.(P) -> Unit)? = null

    var nullableSuspendWithAnnotation: (@A() suspend (P) -> Unit)? = null
}

interface P
interface RS

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class A

class Continuation<T> {}