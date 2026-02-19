// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// ^Verify that no anonymous class is generated KT-63329 KT-62858, old backend gets this wrong.

// FILE: kt49226.kt
fun box(): String {
    val x: Array<String> = Mapper("OK").map(::arrayOf)
    return x[0]
}


// FILE: Func.java
public interface Func <T, R> { R apply(T t); }

// FILE: Mapper.java
public class Mapper<T> {
    T t;
    public Mapper(T t) {
        this.t = t;
    }
    public <R> R map(Func<T, R> f) {
        return f.apply(t);
    }
}
