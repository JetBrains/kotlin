// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidExposingPackagePrivateInInternal
// FILE: javapackage/PackagePrivateGrandparentAbstractClass.java
package javapackage;

/*package-private*/ abstract class PackagePrivateGrandparentAbstractClass {
    public static void publicStaticMethod() {}
}

// FILE: javapackage/KotlinParentClass.kt
package javapackage

internal open class KotlinParentClass : <!EXPOSED_PACKAGE_PRIVATE_TYPE_FROM_INTERNAL_WARNING!>PackagePrivateGrandparentAbstractClass<!>()

// FILE: Child.kt
import javapackage.KotlinParentClass

internal class Child : KotlinParentClass() {
    fun foo() {
        <!INVISIBLE_REFERENCE!>publicStaticMethod<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, javaType */
