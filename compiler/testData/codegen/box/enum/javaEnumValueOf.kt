// TARGET_BACKEND: JVM
// FILE: E.java
public enum E {
    OK();
    public static String valueOf(E x) {
        return x.toString();
    }
}

// FILE: test.kt

// check that both 'valueOf(String): E' and 'valueOf(E): String' are invoked correctly
fun box() =
    E.valueOf(E.valueOf("OK"))
