// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    public static void run(Runnable r) {
        r.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    var v = "FAIL"
    val f = { v = "OK" }
    JavaClass.run(f)
    return v
}
