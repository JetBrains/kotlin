// FIR_IDENTICAL
// FILE: javapackage/PackagePrivateGrandparentAbstractClass.java

package javapackage;

/*package-private*/ abstract class PackagePrivateGrandparentAbstractClass {
    public static void publicStaticMethod() {}
}

// FILE: javapackage/PublicParentClass.java

package javapackage;

public class PublicParentClass extends PackagePrivateGrandparentAbstractClass {}

// FILE: foo.kt

import javapackage.PublicParentClass

typealias TypeAliasedParent = PublicParentClass

fun foo() {
    TypeAliasedParent.publicStaticMethod()
}

class Child : TypeAliasedParent() {
    fun foo() {
        TypeAliasedParent.publicStaticMethod()
        publicStaticMethod()
    }
}
