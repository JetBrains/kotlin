// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +ReferencesToSyntheticJavaProperties

// FILE: Generic.java
public class Generic<T> {
    public String getStringVal() { return null; }
    public void setStringVal(String value) {}

    public T getGenericVal() { return null; }
    public void setGenericVal(T value) {}
}

// FILE: main.kt
fun box(): String {
    Generic<Number>::stringVal
    Generic<Number>::genericVal

    val o = Generic<Number>()
    o::stringVal
    o::genericVal

    return "OK"
}