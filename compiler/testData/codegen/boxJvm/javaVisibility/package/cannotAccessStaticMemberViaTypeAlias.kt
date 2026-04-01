// TARGET_BACKEND: JVM_IR
// FILE: javapackage/PackagePrivateGrandparentAbstractClass.java

package javapackage;

/*package-private*/ abstract class PackagePrivateGrandparentAbstractClass {
    public static String publicStaticMethod() {
        return "OK";
    }
}

// FILE: javapackage/PublicParentClass.java

package javapackage;

public class PublicParentClass extends PackagePrivateGrandparentAbstractClass {}

// FILE: foo.kt

import javapackage.PublicParentClass

typealias TypeAliasedParent = PublicParentClass

fun box() = TypeAliasedParent.publicStaticMethod()
