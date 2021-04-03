// FILE: Callable.java

public interface Callable<V> {
    V call() throws Exception;
}

// FILE: Future.java

public class Future<T> {}

// FILE: Executor.java

public interface Executor {
    <T> Future<T> submit(Callable<T> task);
    Future<?> submit(Runnable task);
}

// FILE: test.kt

fun f(): String = "test"

class A {
    // bug: type(e.submit(::f)) = ft<Future<*>, Future<*>?>,
    // it isn't subtype of Future<String>
    fun schedule1(e: Executor): Future<String> = <!RETURN_TYPE_MISMATCH!>e.submit(::f)<!>

    // this behaviour is OK
    fun schedule2(e: Executor): Future<String> = <!RETURN_TYPE_MISMATCH!>e.submit { f() }<!>
}