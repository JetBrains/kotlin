// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    void invoke(Runnable i) {
        i.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj({ v = "O" })
    obj { v += "K" }
    return v
}
