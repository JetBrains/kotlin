// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: J.java

public class J extends java.util.AbstractCollection<String> {
    private static java.util.List<String> underlying = java.util.Arrays.asList("FAIL");

    public java.util.Iterator<String> iterator() {
        return underlying.iterator();
    }

    public int size() {
        return underlying.size();
    }

    public Object[] toArray() {
        return new String[] { "O" };
    }

    public <T> T[] toArray(T[] a) {
        return a;
    }
}

// FILE: test.kt

// K must not override toArray, since there is an existing implementation coming from Java with different
// behavior than the default implementation.
class K : J()

fun box(): String {
    val k = K() as java.util.Collection<String>
    return (k.toArray()[0] as String) + (k.toArray<String>(arrayOf("K"))[0] as String)
}
