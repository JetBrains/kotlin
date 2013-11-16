// FILE: base/Base.java
package base;

public class Base {
    private static final int PRIVATE = 1;
    protected static final int PROTECTED = 1;
    /*package*/ static final int PACKAGE = 1;
    public static final int PUBLIC = 1;
}

// FILE: derived/Derived.java

package derived;

public class Derived extends base.Base {
}

// FILE: test.kt

import base.*
import derived.*

fun test() {
    Base.<!UNRESOLVED_REFERENCE!>PRIVATE<!>
    Base.<!INVISIBLE_MEMBER!>PROTECTED<!>
    Base.<!INVISIBLE_MEMBER!>PACKAGE<!>
    Base.PUBLIC

    Derived.<!UNRESOLVED_REFERENCE!>PRIVATE<!>
    Derived.<!INVISIBLE_MEMBER!>PROTECTED<!>
    Derived.<!UNRESOLVED_REFERENCE!>PACKAGE<!>
    Derived.PUBLIC
}

class Subclass : Derived() {
    fun test() {
        Base.PROTECTED
        Derived.PROTECTED
    }
}