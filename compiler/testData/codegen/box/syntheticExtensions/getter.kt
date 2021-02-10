// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    public String getOk() { return "OK"; }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    return JavaClass().ok
}
