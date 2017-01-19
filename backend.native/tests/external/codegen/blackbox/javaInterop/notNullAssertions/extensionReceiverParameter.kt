// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FILE: Test.java

public class Test {
    public static String invokeFoo() {
        try {
            ExtensionKt.foo(null);
        }
        catch (IllegalArgumentException e) {
            try {
                ExtensionKt.getBar(null);
            }
            catch (IllegalArgumentException f) {
                return "OK";
            }
        }

        return "Fail: assertion must have been fired";
    }
}

// FILE: extension.kt

fun Any.foo() { }

val Any.bar: String get() = ""

fun box(): String {
    return Test.invokeFoo()
}
