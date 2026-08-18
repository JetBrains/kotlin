// FILE: Constructor.java
package test;

class JavaClass {
    void testMethod() {
        new None();

        try {
            new One();
        }
        catch (E1 e) {}

        try {
            new OneWithParam(1);
        }
        catch (E1 e) {}

        try {
            new Two();
        }
        catch (E1 e) {}
        catch (E2 e) {}
    }
}

// FILE: Constructor.kt
package test

class E1: Exception()
class E2: Exception()

class None @Throws() constructor() {}
class One @Throws(E1::class) constructor()
class Two @Throws(E1::class, E2::class) constructor()

class OneWithParam @Throws(E1::class) constructor(a: Int)
