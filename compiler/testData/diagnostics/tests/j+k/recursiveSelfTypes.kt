// FIR_IDENTICAL
// SKIP_TXT
// FILE: TSFBuilder.java
public abstract class TSFBuilder<F extends CharSequence, B extends TSFBuilder<F,B>> {
    public B configure() { return null; }
    public abstract F build();

    public static TSFBuilder<?, ?> start() { return null; }
}

// FILE: test.kt

fun foo(x: CharSequence?) {}

fun main() {
    foo(
        TSFBuilder.start()
            .configure()
            .configure()
            .configure()
            .configure()
            .configure()
            .configure()
            .configure()
            .configure()
            .configure()
            .configure()
            .configure()
            .build()
    )
}
