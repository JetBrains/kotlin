// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// SCOPE_OWNER: C
// PREWARM_SCOPES_WITHOUT_REQUIRED_PHASE: C
// FILE: B.kt
abstract class B<T> {
    fun bar(t: T) {}
    abstract fun foo(t: T)
    val baz: T get() = TODO()
}

// FILE: C.java
public abstract class C extends B<String> {
}
