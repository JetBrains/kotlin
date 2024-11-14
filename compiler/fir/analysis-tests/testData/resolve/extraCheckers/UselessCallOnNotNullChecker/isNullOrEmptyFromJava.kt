// RUN_PIPELINE_TILL: BACKEND
// FILE: SomeUtil.java

public class SomeUtil {
    public static boolean isNullOrEmpty(Object value) {
        return value == null;
    }
}

// FILE: main.kt

interface Foo

fun test(p: Foo) {
    SomeUtil.isNullOrEmpty(p)
}
