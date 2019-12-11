// JAVAC_EXPECTED_FILE
// FILE: test/My.java

package test;

class Internal {}

public class My {
    static public Internal foo() { return new Internal(); }
}

// FILE: test/His.kt

package test

class His {
    // Ok: private vs package-private
    private fun private() = My.foo()
    // Ok: internal vs package-private in same package
    internal fun internal() = My.foo()
    // Error: protected vs package-private
    protected fun protected() = My.foo()
    // Error: public vs package-private
    fun public() = My.foo()
}

// FILE: other/Your.kt

package other

import test.My

class Your {
    internal fun bar() = My.foo()
}
