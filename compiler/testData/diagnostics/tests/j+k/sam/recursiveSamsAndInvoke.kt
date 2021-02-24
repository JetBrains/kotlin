// FIR_IDENTICAL
// FILE: MyFuture.java

public interface MyFuture<V> {
    MyFuture<V> addListener(MyListener<? extends MyFuture<? super V>> listener);
}

// FILE: MyListener.java

public interface MyListener<F extends MyFuture<?>>  {
    void operationComplete(F future) throws Exception;
}

// FILE: test.kt

typealias Handler = (cause: Throwable?) -> Unit

fun <T> MyFuture<T>.setup() {
    addListener(ListenerImpl<T, MyFuture<T>>())
    addListener { }
}

class ListenerImpl<T, F : MyFuture<T>> : MyListener<F>, Handler {
    override fun operationComplete(future: F) {
    }

    override fun invoke(cause: Throwable?) {
    }
}