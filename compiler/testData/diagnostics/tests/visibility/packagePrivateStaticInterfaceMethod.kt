// FIR_IDENTICAL
// FILE: javapackage/PackagePrivateGrandparentAbstractClass.java

package javapackage;

/*package-private*/ interface PackagePrivateGrandparentInterface {
    static void publicStaticMethod() {}

    String publicStaticField = "OK";
}

// FILE: javapackage/PublicParentClass.java

package javapackage;

public class PublicParentClass implements PackagePrivateGrandparentInterface {}

// FILE: Child.kt

import javapackage.PublicParentClass

class Child : PublicParentClass() {
    fun foo(): String {
        <!UNRESOLVED_REFERENCE!>publicStaticMethod<!>()                   // Error!
        PublicParentClass.<!UNRESOLVED_REFERENCE!>publicStaticMethod<!>() // Error!
        return publicStaticField                                          // Ok!
    }
}
