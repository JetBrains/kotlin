// TARGET_BACKEND: JVM_IR
// ISSUE: KT-71166

// FILE: Base.java
public abstract class Base<T> {
    public String result = "";

    public T getValue() {
        result += "O";
        return (T) kotlin.Unit.INSTANCE;
    }

    protected void setValue(T value) {
        result += "K";
    }
}

// FILE: main.kt
class DerivedWithOverride<T : Any> : Base<T>() {
    override fun getValue(): T {
        return super.getValue()
    }
    public override fun setValue(value: T) {
        super.setValue(value)
    }
}

class DerivedWithoutOverride<T : Any> : Base<T>()

fun box(): String {
    val x = DerivedWithOverride<Unit>()
    x.value
    x.value = Unit
    if (x.result != "OK") return x.result

    val y = DerivedWithoutOverride<Unit>()
    y.value
    y.value = Unit
    if (y.result != "OK") return y.result

    return "OK"
}
