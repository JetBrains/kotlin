// FILE: JavaClass.java

class JavaClass {
    int get(Runnable i) {
        i.run();
        return 239;
    }
}

// FILE: 1.kt

fun box(): String {
    val obj = JavaClass()

    var v = "FAIL"
    obj[{ v = "OK" }]
    return v
}
