// FILE: JavaClass.java

class JavaClass {
    void set(Runnable i, Runnable value) {
        i.run();
        value.run();
    }
}

// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj[{ v = "O" }] = { v += "K" }
    return v
}
