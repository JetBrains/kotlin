// TARGET_BACKEND: JVM

// FILE: unresolvedJavaClassInDifferentFile.kt
import j.Base

class Derived : Base() {
    fun ok() = "OK"
}

fun box() =
    Derived().ok()

// FILE: j/Foo.java
package j;

// NB package-private class 'j.Bar' in file 'j/Foo.java'
class Bar {
}

// FILE: j/Base.java
package j;

public class Base {
    protected Bar bar() {
        return new Bar();
    }

    protected void bar(Bar b) {
    }
}