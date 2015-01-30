// FILE: p/Base.java

package p;

public class Base<T> {
    void foo(R<?> r) {}
}

// FILE: k.kt
package p

class R<T: R<T>>

class Derived: p.Base<String>()