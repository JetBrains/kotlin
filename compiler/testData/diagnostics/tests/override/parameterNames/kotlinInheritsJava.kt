// FILE: JavaInterface.java

interface JavaInterface {
    void foo(int javaName);
}

// FILE: kotlin.kt

class SimpleSubclass : JavaInterface {
    override fun foo(kotlinName: Int) {}
}


trait SubtraitWithFakeOverride : JavaInterface

class Subclass : SubtraitWithFakeOverride {
    override fun foo(otherKotlinName: Int) {}
}
