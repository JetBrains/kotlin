// FILE: TopLevel.java
package test;

class JavaClass {
    void testMethod() {
        TopLevelKt.none();

        try {
            TopLevelKt.one();
        }
        catch (E1 e) {}

        try {
            TopLevelKt.two();
        }
        catch (E1 e) {}
        catch (E2 e) {}
    }
}

// FILE: TopLevel.kt
package test

class E1: Exception()
class E2: Exception()

@Throws()
fun none() {}

@Throws(E1::class)
fun one() {}

@Throws(E1::class, E2::class)
fun two() {}
