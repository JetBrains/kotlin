// JVM_DEFAULT_MODE: disable
// LANGUAGE: +JvmEnhancedBridges

// FILE: JInterface.java
public interface JInterface {
    public abstract Integer bar();
}

// FILE: KInterfaces.kt
interface I1 {
    fun foo(): Int
    fun bar(): Int
}

interface I2: JInterface {
    override fun bar(): Int // effectively stays Int? (Integer) because it is from Java interface originally
    // no bridges, as real signature is the same
}


// File: Test.kt
interface T1<T: Int?>: JInterface {
    override fun bar(): T & Any // looks like a definitely-non-nullable, but actually stays Int?
    // no bridges, as real signature is the same
}

interface T2<T: Int?>: I1, JInterface {
    override fun foo(): T & Any
    override fun bar(): T & Any
    // both bridges from I1
}

interface TI3 : I1, I2 {
    override fun foo(): Nothing
    override fun bar(): Nothing
    // int foo() from I1
    // int bar() from I1
    // java.lang.Integer bar() from I2
}

interface TI4 : I1, JInterface {
    override fun foo(): Nothing
    override fun bar(): Nothing
    // int foo() from I1
    // int bar() from I1
    // java.lang.Integer bar() from JInterface
}

interface TI5 : I1, I2, JInterface {
    override fun foo(): Nothing
    override fun bar(): Nothing
    // int foo(); from I1
    // int bar(); from I1
    // java.lang.Integer bar(); from I2
}
