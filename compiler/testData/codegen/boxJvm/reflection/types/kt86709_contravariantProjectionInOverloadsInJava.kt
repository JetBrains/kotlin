// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/Subscriber.java
package test;

public interface Subscriber<T> {}

// FILE: test/CorePublisher.java
package test;

public interface CorePublisher<T> extends Publisher<T> {
    void subscribe(CoreSubscriber<? super T> s);
}

// FILE: test/Publisher.java
package test;

public interface Publisher<T> {
    void subscribe(Subscriber<? super T> s);
}

// FILE: test/CoreSubscriber.java
package test;

public interface CoreSubscriber<T> extends Subscriber<T> {}

// FILE: test/Mono.java
package test;

public abstract class Mono<T> implements CorePublisher<T> {
    public void subscribe(CoreSubscriber<? super T> actual) {}
    public void subscribe(Subscriber<? super T> actual) {}
}

// FILE: box.kt
import test.*
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(
        listOf(
            "fun test.Mono<T>.subscribe(test.CoreSubscriber<in T!>!): kotlin.Unit",
            "fun test.Mono<T>.subscribe(test.Subscriber<in T!>!): kotlin.Unit",
        ),
        Mono::class.members.filter { it.name == "subscribe" }.map { it.toString() }.sorted()
    )

    return "OK"
}
