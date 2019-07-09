// TARGET_BACKEND: JVM

// FILE: JavaClass.java

public class JavaClass extends A {
    public String test() {
        return "Java";
    }
}

// FILE: test.kt

open class A {
    internal open fun test(): String = "Kotlin"
}

fun box(): String {
    if (A().test() != "Kotlin") return "fail 1: ${A().test()}"

    if (JavaClass().test() != "Java") return "fail 2: ${JavaClass().test()}"

    return "OK"
}
