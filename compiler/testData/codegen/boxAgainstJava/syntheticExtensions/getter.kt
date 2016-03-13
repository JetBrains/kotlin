// FILE: JavaClass.java

class JavaClass {
    public String getOk() { return "OK"; }
}

// FILE: 1.kt

fun box(): String {
    return JavaClass().ok
}
