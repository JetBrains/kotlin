// FILE: DefaultArgs.java
package test;

class JavaClass {
    void testMethod(One instance) {
        try {
            new One(1);
        }
        catch (E1 e) {}

        try {
            new One();
        }
        catch (E1 e) {}

        try {
            DefaultArgsKt.one(1);
        }
        catch (E1 e) {}

        try {
            DefaultArgsKt.one();
        }
        catch (E1 e) {}
    }
}

// FILE: DefaultArgs.kt
package test

class E1: Exception()

@Throws(E1::class) @JvmOverloads
fun one(a: Int = 1) {}

class One @Throws(E1::class) constructor(a: Int = 1) {
    @Throws(E1::class)
    fun one(a: Int = 1) {}
}
