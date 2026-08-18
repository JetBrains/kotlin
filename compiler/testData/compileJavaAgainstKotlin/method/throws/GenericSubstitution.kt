// FILE: GenericSubstitution.java
package test;

class JavaClass {
    public static void main(String[] args) {
        try {
            new Derived().one(null);
        }
        catch (E1 e) {}
    }
}

// FILE: GenericSubstitution.kt
package test

class E1: Exception()

interface Base<T> {
    @Throws(E1::class)
    fun one(t: T) {}
}

class Derived: Base<String>
