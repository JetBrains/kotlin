// FILE: JavaClass.java

class JavaClass {
    public void run(Runnable r) {
        r.run();
    }
}

// FILE: 1.kt

internal class KotlinSubclass: JavaClass() {
}

fun box(): String {
    var v = "FAIL"
    KotlinSubclass().run { v = "OK" }
    return v
}
