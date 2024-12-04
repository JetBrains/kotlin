// TARGET_BACKEND: JVM
// SAM_CONVERSIONS: CLASS
// FILE: S.java
public interface S {
    String bar(String t);

    S RESULT = new S() {
        @Override
        public String bar(String t) { return t; }
    };
}

// FILE: J.java
public class J {
    public static String foo(String e, S c) {
        return c.bar(e);
    }
}

// FILE: box.kt
fun box(): String {
    val s: (String) -> String = { S.RESULT.bar(it) }
    return J.foo("OK", S(s))
}
