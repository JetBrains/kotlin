package test

class SuspendLambda {
    fun <T> (suspend () -> T).createCoroutine(completion: Continuation<T>) {}

    fun <T> testCoroutine(f: suspend (Int) -> T?) {}

    fun <T> testCoroutineWithAnnotation(f: suspend (Int) -> @A T?) {}
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class A

class Continuation<T> {}