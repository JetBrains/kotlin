// FIR_IDENTICAL
// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM
// NULLABILITY_ANNOTATIONS: @io.reactivex.rxjava3.annotations:strict
// IGNORE_LIGHT_ANALYSIS

// FILE: MyBiConsumer.java
import io.reactivex.rxjava3.annotations.NonNull;

@FunctionalInterface
public interface MyBiConsumer<@NonNull T> {
    void accept(T t);
}

// FILE: MyMaybe.java
import io.reactivex.rxjava3.annotations.Nullable;

public class MyMaybe {
    public static void doOnEvent(MyBiConsumer<@Nullable ? super Throwable> onEvent) {
        onEvent.accept(null);
    }
}

// FILE: main.kt
fun box(): String {
    MyMaybe.doOnEvent { _ -> }
    return "OK"
}
