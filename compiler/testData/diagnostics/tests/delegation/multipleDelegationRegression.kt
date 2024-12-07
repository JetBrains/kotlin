// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-71215

// FILE: Base.java
public interface Base {
    String method();
}

// FILE: main.kt
interface KotlinDerived : Base

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Bottom<!>(val j: Base, val k: KotlinDerived) : Base by j, KotlinDerived by k
