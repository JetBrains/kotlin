// IGNORE_BACKEND: JVM_IR
//WITH_RUNTIME
// FILE: JavaClass.java

public class JavaClass {

    public Double test() {
        return null;
    }

}

// FILE: b.kt

fun test(s: Double) {

}

fun box(): String {
    try {
        test(JavaClass().test())
    }
    catch (e: IllegalStateException) {
        return "OK"
    }
    return "fail"
}