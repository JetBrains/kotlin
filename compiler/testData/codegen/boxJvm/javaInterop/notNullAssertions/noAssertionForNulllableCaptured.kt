// ISSUE: KT-71122
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: MyConsumer.java
public interface MyConsumer<T> extends java.util.function.Consumer<T> {
    void consume(T t);

    @Override
    default void accept(T t) {
        consume(t);
    }
}

// FILE: NullableConsumer.java
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface NullableConsumer<T> extends MyConsumer<T>, java.util.function.Consumer<T> {
    @Override
    void consume(@Nullable T t);

    @Override
    default void accept(@Nullable T t) {
        consume(t);
    }
}

// FILE: Foo.java
public class Foo {
    public static String flexibleString() { return null; }
}

// FILE: main.kt

var result = "fail"

private fun createNullableConsumer(): NullableConsumer<String> = object : NullableConsumer<String> {
    override fun consume(t: String?) {
        result = t ?: "OK"
    }
}

fun box(): String {
    val consumer: NullableConsumer<in String> = createNullableConsumer()
    val foo/*: String! */ = Foo.flexibleString()
    consumer.consume(foo)

    return result
}
