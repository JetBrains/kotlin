// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    public static void run(Runnable r1, Runnable r2) {
        r1.run();
        r2.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    var v = "FAIL"
    val f = { v = "O" }
    JavaClass.run(f, { v += "K" })
    return v
}
