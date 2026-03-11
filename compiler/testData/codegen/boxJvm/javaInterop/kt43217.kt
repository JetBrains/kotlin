// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8

// FILE: kt43217.kt

class A {
    private val b =
        object : DoubleExpression() {
            override fun get(): Double {
                return 0.0
            }
        }

    val test: DoubleExpression = b
}

class C : DoubleExpression() {
    override fun get() = 0.0
}

fun box(): String {
    if (A().test.get() != 0.0) throw AssertionError("A().test.get() != 0.0")

    if (C().get() != 0.0) throw AssertionError("C().get() != 0.0")

    return "OK"
}

// FILE: DoubleExpression.java
import org.jetbrains.annotations.NotNull;

public abstract class DoubleExpression implements ObservableDouble {
    @NotNull
    @Override
    public Object isEqualTo(double value) {
        return null;
    }
}

// FILE: ObservableValue.java
import org.jetbrains.annotations.NotNull;

public interface ObservableValue<T> {
    @NotNull
    T get();

    @NotNull
    default Object isEqualTo(@NotNull T value) {
        return null;
    }
}

// FILE: ObservableDouble.java
import org.jetbrains.annotations.NotNull;

public interface ObservableDouble extends ObservableValue<Double> {
    @NotNull
    Object isEqualTo(double value);
}

