// FILE: Query.java

public interface Query {
    Stream getResultStream();
}

// FILE: Stream.java

import java.util.function.Function;

public interface Stream<T> {
    <R> Stream<R> map(Function<? super T, ? extends R> mapper);
}

// FILE: main.kt

fun foo(query: Query) {
    query.resultStream.map { it as Array<*> } // NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
}

fun box(): String {
    return "OK"
}