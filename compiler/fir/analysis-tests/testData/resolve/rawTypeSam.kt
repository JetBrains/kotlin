// FILE: main.kt

fun foo() {
    RawType.bar {
        it.length > 0
    }
}

// FILE: Processor.java
public interface Processor<T extends CharSequence> {
    boolean process(T t);
}

// FILE: RawType.java
public class RawType {
    public static void bar(Processor x) {}
}
