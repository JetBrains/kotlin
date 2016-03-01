// FILE: JavaClass.java

class JavaClass {
    JavaClass(Runnable r) {
        if (r != null) r.run();
    }
}

// FILE: 1.kt

internal class KotlinClass(): JavaClass(null) {
}

fun box(): String {
    KotlinClass()
    return "OK"
}
