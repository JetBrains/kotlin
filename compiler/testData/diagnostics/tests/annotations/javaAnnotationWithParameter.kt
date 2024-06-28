// FIR_IDENTICAL
// ISSUE: KT-64083
// FILE: ThreadSafe.java

public @interface ThreadSafe {
    String reason() default "";
}

// FILE: test.kt

private annotation class AutoFactoryThreadSafe(
    val threadSafe: ThreadSafe = ThreadSafe()
)
