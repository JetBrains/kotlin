// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: JavaClass.java

public class JavaClass {

    public Double test() {
        return null;
    }

}

// MODULE: main(lib)
// FILE: b.kt

fun test(s: Double) {

}

fun box(): String {
    try {
        test(JavaClass().test())
    }
    catch (e: NullPointerException) {
        return "OK"
    }
    return "fail"
}
