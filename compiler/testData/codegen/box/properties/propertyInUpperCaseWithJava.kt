// TARGET_BACKEND: JVM_IR
// ISSUE: KT-61972

// FILE: Base.kt
interface Base {
    val URL: String
}

// FILE: Derived.java
public class Derived implements Base {
    private String value;

    public Derived(String value) {
        this.value = value;
    }

    @java.lang.Override
    public String getURL() {
        return value;
    }
}

// FILE: main.kt
fun box(): String {
    val d1 = Derived("O")
    val d2 = Derived("K")
    return d1.URL + d2.getURL()
}
