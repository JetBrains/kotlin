// FILE: p1/BaseClass.java

package p1;

public class BaseClass {
    protected class ProtSubClass {
        public ProtSubClass() {}
    }
}

// FILE: k1/main.kt
package k1

import p1.BaseClass

class Foo : BaseClass() {

    fun foo() {
        ProtSubClass()
        super.ProtSubClass()
    }

    private val v1: BaseClass.ProtSubClass = ProtSubClass()
}
