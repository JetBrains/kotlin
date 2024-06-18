// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-69215

interface Callback {
    fun foo()
}

interface SuccessCallback<in T1> {
    fun onSuccess(value: T1)
}

interface MaybeCallbacks<in T2> : SuccessCallback<T2>, Callback

interface Disposable

interface Observer {
    fun onSubscribe(disposable: Disposable)
}

interface MaybeObserver<in T3> : Observer, MaybeCallbacks<T3>

fun <T7, R7> test(emitter: MaybeCallbacks<R7>) {
    object : MaybeObserver<T7>, Callback by emitter {
        override fun onSubscribe(disposable: Disposable) {}

        override fun onSuccess(value: T7) {
            object : MaybeObserver<R7>, Observer by this, MaybeCallbacks<R7> by emitter {}
        }
    }
}

fun box(): String = "OK"
