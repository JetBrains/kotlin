// FILE: JavaClass.java

class JavaClass {
    void invoke(Runnable i) {
        i.run();
    }
}

// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj({ v = "O" })
    obj { v += "K" }
    return v
}
