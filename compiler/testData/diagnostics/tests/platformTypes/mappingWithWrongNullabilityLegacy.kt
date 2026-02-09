// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78783
// LANGUAGE: -DontMakeExplicitNullableJavaTypeArgumentsFlexible -PreciseSimplificationToFlexibleLowerConstraint
// JVM_TARGET: 1.8
// FULL_JDK
// FIR_DUMP

// FILE: Subscriber.java

public interface Subscriber<S> {}

// FILE: Subscribers.java

public class Subscribers {
    public static Subscriber<java.io.InputStream> of() { return null; }

    public static <T, U> Subscriber<U> mapping(Subscriber<T> s, java.util.function.Function<? super T, ? extends U> mapper) { return null; }
}

// FILE: main.kt

import java.io.InputStream
import java.util.zip.GZIPInputStream

class Handler<H>(val reader: (s: InputStream) -> H) {
    fun foo(f: Boolean): Subscriber<H> {
        val inputSubscriber = Subscribers.of()

        val subscriber = if (f) {
            Subscribers.mapping<InputStream?, InputStream?>(inputSubscriber, ::GZIPInputStream)
        } else {
            inputSubscriber
        }

        return Subscribers.mapping(subscriber) {
            reader(it)
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration */
