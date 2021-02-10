// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    JavaClass(Runnable r) {
        if (r != null) r.run();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

internal class KotlinClass(): JavaClass(null) {
}

fun box(): String {
    KotlinClass()
    return "OK"
}
