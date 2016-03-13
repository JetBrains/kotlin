// FILE: JavaClass.java

class JavaClass {
    boolean contains(Runnable i) {
        i.run();
        return true;
    }
}

// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    { v = "O" } in obj
    { v += "K" } !in obj
    return v
}
