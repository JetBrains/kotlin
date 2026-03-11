// TARGET_BACKEND: JVM_IR
// FILE: javapackage/PackagePrivateGrandparentAbstractClass.java

package javapackage;

/*package-private*/ abstract class PackagePrivateGrandparentAbstractClass {
    public static void publicStaticMethod() {}

    public static String publicStaticField = "OK";
}

// FILE: javapackage/PublicParentClass.java

package javapackage;

public class PublicParentClass extends PackagePrivateGrandparentAbstractClass {}

// FILE: Child.kt

typealias Base = javapackage.PublicParentClass

class Child : Base() {
    fun foo(): String {
        publicStaticMethod()
        return publicStaticField
    }
}

// FILE: test.kt

fun box() = Child().foo()
