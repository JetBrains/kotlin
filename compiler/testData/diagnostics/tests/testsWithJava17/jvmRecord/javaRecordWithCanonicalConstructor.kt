// FIR_IDENTICAL

// FILE: WithCompactCtor.java
public record WithCompactCtor(String s) {
    public WithCompactCtor {}
}

// FILE: WithExplicitCanonicalCtor.java
public record WithExplicitCanonicalCtor(String s) {
    public WithExplicitCanonicalCtor(String s) {
        this.s = s;
    }
}

// FILE: test.kt
fun test() {
    WithCompactCtor("")
    WithExplicitCanonicalCtor("")
}