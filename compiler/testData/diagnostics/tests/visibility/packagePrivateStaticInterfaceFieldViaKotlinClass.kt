// FIR_IDENTICAL
// FIR_DUMP
// FILE: javapackage/PackagePrivateGrandparentAbstractClass.java

package javapackage;

/*package-private*/ interface PackagePrivateGrandparentInterface {
    public static String publicStaticField = "OK";
}

// FILE: javapackage/KotlinParentClass.kt

package javapackage

class KotlinParentClass : PackagePrivateGrandparentInterface

// FILE: javapackage/PublicParentClass.java

package javapackage;

public class PublicParentClass extends KotlinParentClass {}

// FILE: Child.kt

import javapackage.PublicParentClass
import javapackage.KotlinParentClass

class Child : PublicParentClass() {
    fun foo() {
        val x = publicStaticField
        val y = PublicParentClass.publicStaticField
        val z = KotlinParentClass.<!UNRESOLVED_REFERENCE!>publicStaticField<!>
    }
}
