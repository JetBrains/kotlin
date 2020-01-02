// FILE: JavaInterface.java

interface JavaInterface {
    void foo(int javaName);
}

// FILE: kotlin.kt

interface KotlinTrait {
    public fun foo(someOtherName: Int) {}
}

class BothTraitsSubclass : JavaInterface, KotlinTrait