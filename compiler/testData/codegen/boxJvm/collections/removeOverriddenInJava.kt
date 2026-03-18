// TARGET_BACKEND: JVM
// FILE: removeOverriddenInJava.kt

open class A : Collection<String> {
    override val size: Int get() = TODO()
    override fun contains(element: String): Boolean = TODO()
    override fun containsAll(elements: Collection<String>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<String> = TODO()
}

fun box(): String {
    B().remove("OK")
    return B.removed as String
}

// FILE: B.java
public class B extends A {
    public static Object removed = null;

    @Override
    public boolean remove(Object o) {
        removed = o;
        return false;
    }
}