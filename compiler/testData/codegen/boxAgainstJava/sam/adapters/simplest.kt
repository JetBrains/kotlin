// FILE: JavaClass.java

class JavaClass {
    public static void run(Runnable r) {
        r.run();
    }
}

// FILE: 1.kt

fun box(): String {
    var v = "FAIL"
    JavaClass.run { v = "OK" }
    return v
}
