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
    fun schedule1(e: Executor): Future<String> = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>e.submit(::f)<!>
    fun schedule2(e: Executor): Future<String> = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>e.submit { f() }<!>
}
