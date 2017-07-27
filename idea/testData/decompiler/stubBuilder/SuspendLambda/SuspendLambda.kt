package test

class SuspendLambda {
    fun <T> (suspend () -> T).createCoroutine(completion: Continuation<T>) {}

    fun <R, T> (suspend R.() -> T).createCoroutine(receiver: R, completion: Continuation<T>) {}

    fun <R, T> (suspend @receiver:A R.() -> T).createCoroutineAnother() {}

    fun <T> testCoroutine(f: suspend (Int) -> T?) {}

    fun <T> testCoroutineWithAnnotation(f: suspend (Int) -> @A T?) {}
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER)
annotation class A

class Continuation<T> {}