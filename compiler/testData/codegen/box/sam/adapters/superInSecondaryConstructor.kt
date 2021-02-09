// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: Supplier.java
public interface Supplier<T> {
    T get();
}

// FILE: Base.java
public class Base {
    private final Supplier<String> supplier;

    public Base(Supplier<String> supplier) {
        this.supplier = supplier;
    }

    public String get() {
        return supplier.get();
    }
}

// MODULE: main(lib)
// FILE: test.kt
class Test : Base {
    constructor(f: () -> String) : super(f)
}

fun box() = Test({ "OK" }).get()
