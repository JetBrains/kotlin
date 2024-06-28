// FIR_IDENTICAL
// FULL_JDK
// JVM_TARGET: 1.8

// FILE: test.kt

fun waitTestConnection(res: B<*>) {
    C.wait(res.toFuture())
}

// FILE: B.kt

import java.util.concurrent.CompletableFuture

abstract class B<S> {
    abstract fun toFuture(): CompletableFuture<S>
}

// FILE: C.java

import java.util.concurrent.Future;

public class C {
    public static <F> F wait(Future<F> future) {
        return null;
    }
}
