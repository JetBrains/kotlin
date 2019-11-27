// IGNORE_BACKEND_FIR: JVM_IR
public abstract class AbstractClass<T> {
    public abstract val some: T
}

public class Class: AbstractClass<String>() {
    public override val some: String
        get() = "OK"
}

fun box(): String = Class().some
