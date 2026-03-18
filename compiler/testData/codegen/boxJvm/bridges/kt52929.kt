// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: lib.kt

open class KotlinCollection<T> : Collection<T> by emptyList<T>()
class BreakGenericSignatures : KotlinCollection<String>()

// FILE: JavaCollection.java

public class JavaCollection extends KotlinCollection<String> {
    public String result() { return "OK"; }
}

// FILE: main.kt

fun box(): String = JavaCollection().result()