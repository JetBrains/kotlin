// ISSUE: KT-71215

// FILE: Base.java
public interface Base {
    String method();
}

// FILE: main.kt
interface KotlinDerived : Base

class Bottom(val j: Base, val k: KotlinDerived) : Base by j, KotlinDerived by k
