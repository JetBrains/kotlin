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
    protected fun <!EXPOSED_FUNCTION_RETURN_TYPE!>protected<!>() = My.foo()
    // Error: public vs package-private
    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>public<!>() = My.foo()
}

// FILE: other/Your.kt

package other

import test.My

class Your {
    // Ok but dangerous: internal vs package-private in different package
    internal fun bar() = My.foo()
}
