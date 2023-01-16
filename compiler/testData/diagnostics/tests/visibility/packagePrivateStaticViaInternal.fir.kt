// FILE: javapackage/PackagePrivateGrandparentAbstractClass.java
package javapackage;

/*package-private*/ abstract class PackagePrivateGrandparentAbstractClass {
    public static void publicStaticMethod() {}
}

// FILE: javapackage/KotlinParentClass.kt
package javapackage

internal open class KotlinParentClass : PackagePrivateGrandparentAbstractClass()

// FILE: Child.kt
import javapackage.KotlinParentClass

internal class Child : KotlinParentClass() {
    fun foo() {
        <!INVISIBLE_REFERENCE!>publicStaticMethod<!>()
    }
}
