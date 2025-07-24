// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: Delegation to stdlib class annotated with @MustUseReturnValue (KT-79125)
// FILE: lib.kt

open class KotlinCollection<T> : Collection<T> by emptyList<T>()
class BreakGenericSignatures : KotlinCollection<String>()

// FILE: JavaCollection.java

public class JavaCollection extends KotlinCollection<String> {
    public String result() { return "OK"; }
}

// FILE: main.kt

fun box(): String = JavaCollection().result()
