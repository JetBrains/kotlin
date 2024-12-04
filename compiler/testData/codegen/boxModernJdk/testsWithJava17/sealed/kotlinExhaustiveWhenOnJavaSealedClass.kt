// TARGET_BACKEND: JVM_IR
// ISSUE: KT-56549

// FILE: SealedJava.java
public abstract sealed class SealedJava permits SubSealedAJava, SubSealedBJava {}

// FILE: SubSealedAJava.java
public final class SubSealedAJava extends SealedJava {}

// FILE: SubSealedBJava.java
public non-sealed class SubSealedBJava extends SealedJava {}

// FILE: main.kt
fun test(sj: SealedJava) = when (sj) {
    is SubSealedAJava -> "O"
    is SubSealedBJava -> "K"
}

fun box(): String {
    return test(SubSealedAJava()) + test(SubSealedBJava())
}
