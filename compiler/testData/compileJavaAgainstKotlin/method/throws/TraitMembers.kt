// FILE: TraitMembers.java
package test;

class JavaClass {
    void testMethod() {
        Test test = new Test();
        test.none();

        try {
            test.one();
        }
        catch (E1 e) {}

        try {
            test.two();
        }
        catch (E1 e) {}
        catch (E2 e) {}
    }
}

// FILE: TraitMembers.kt
package test

class E1: Exception()
class E2: Exception()

interface Trait {
    @Throws()
    fun none() {}

    @Throws(E1::class)
    fun one() {}

    @Throws(E1::class, E2::class)
    fun two() {}
}

class Test: Trait
