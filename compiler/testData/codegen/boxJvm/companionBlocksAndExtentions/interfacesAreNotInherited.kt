// TARGET_BACKEND: JVM
// LANGUAGE: +CompanionBlocks

// FILE: JavaInterface.java
interface JavaInterface {
    static String foo(java.util.List<Integer> x) {
        return "Java";
    }
}

// FILE: test.kt

interface KotlinInterface {
    companion {
        fun foo(x: List<String>) = "Kotlin"
    }
}

class Hide : JavaInterface, KotlinInterface {
    companion {
        fun foo(x: List<Double>) = "OK"
    }
}

fun box(): String {
    return Hide.foo(listOf(1.0))
}
