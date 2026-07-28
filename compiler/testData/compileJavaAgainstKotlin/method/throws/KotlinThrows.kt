// FILE: KotlinThrows.java
package test;

class JavaClass {
    void testMethod() {

        try {
            KotlinThrowsKt.kt();
        }
        catch (E1 e) {}

        try {
            KotlinThrowsKt.ktJvm();
        }
        catch (E2 e) {}
    }
}

// FILE: KotlinThrows.kt
package test

class E1 : Exception()
class E2 : Exception()

@kotlin.Throws(E1::class)
fun kt() {}

@kotlin.jvm.Throws(E2::class)
fun ktJvm() {}
