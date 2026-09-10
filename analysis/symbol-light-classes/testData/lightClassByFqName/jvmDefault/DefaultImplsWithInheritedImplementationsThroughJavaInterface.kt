// B
// LIBRARY_PLATFORMS: JVM
// JVM_DEFAULT_MODE: disable

// FILE: A.kt
interface A {
    fun a() {}

    val prop: Int
        get() = 1

    fun overriddenInJava() {}

    fun reabstractedInJava() {}
}

// FILE: JavaInterface.java
public interface JavaInterface extends A {
    @Override
    default void overriddenInJava() {}

    @Override
    void reabstractedInJava();
}

// FILE: B.kt
interface B : JavaInterface
