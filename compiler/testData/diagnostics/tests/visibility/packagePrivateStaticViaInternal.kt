// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidExposingPackagePrivateInInternal
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
        <!INVISIBLE_MEMBER!>publicStaticMethod<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, javaType */
