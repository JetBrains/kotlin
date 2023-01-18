// FIR_IDENTICAL
// FILE: javapackage/PublicParentInterface.java

package javapackage;

public interface PublicParentInterface {
    String publicStaticField = "OK";
}

// FILE: Child.kt

import javapackage.PublicParentInterface

class Child : PublicParentInterface {
    fun foo(): String {
        return <!UNRESOLVED_REFERENCE!>publicStaticField<!>
    }
}
