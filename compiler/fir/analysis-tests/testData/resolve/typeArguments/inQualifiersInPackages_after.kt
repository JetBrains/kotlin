// ISSUE: KT-83652, KT-84154
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS, +ForbidUselessTypeArgumentsIn25, +ForbidAnnotationsTypeArgumentsAndParenthesesForPackageQualifier

// FILE: part1/part2/GenericJava.java

package part1.part2;

public class GenericJava<T> {
    public static void S() {}
    public void M() {}
}

// FILE: part1/part2/NonGenericJava.java

package part1.part2;

public class NonGenericJava {
    public static void S() {}
    public void M() {}
}

// FILE: part1/part2/part3/ThreeParts.kt

package part1.part2.part3

fun Function(): Unit = Unit

val Variable: Unit = Unit

object Obj

// FILE: part1/part2/TwoParts.kt

package part1.part2

enum class KtEnum {
    E;
}

class Generic<T> {
    fun M() {}
    companion object {
        fun S() {}
    }
}

class NonGeneric {
    fun M() {}
    companion object {
        fun S() {}
    }
}

object Obj {
    fun M() {}
}

class ClassWithNestedObjects {
    object Obj
    class WithCompanion {
        companion object
    }
}

object ObjectWithNestedObjects {
    object Obj
    class WithCompanion {
        companion object
    }
}

class WithInner {
    inner class Inner {
        fun M() {}
    }
}

// FILE: tests.kt

fun testJavaCallableReferences() {
    // for reference:
    part1.part2.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GenericJava<!>::S
    part1.part2.GenericJava<Int>::S
    part1.part2.GenericJava<Int>::M

    // generic:
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.GenericJava::S
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.GenericJava::M
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.GenericJava::S
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.GenericJava::M
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.GenericJava<Int>::S
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.GenericJava<Int>::M
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.GenericJava<Int>::S
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.GenericJava<Int>::M

    // non-generic:
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.NonGenericJava::S
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.NonGenericJava::M
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.NonGenericJava::S
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.NonGenericJava::M
}

fun testJavaStatics() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.GenericJava.S()
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.GenericJava.S()
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.GenericJava<Int>.S()
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.GenericJava<Int>.S()

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.NonGenericJava.S()
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.NonGenericJava.S()
}

fun testKotlinCompanions() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Generic
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Generic

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Generic.toString()
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Generic.toString()

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.NonGeneric
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.NonGeneric

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.NonGeneric.toString()
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.NonGeneric.toString()
}

fun testEnums() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.KtEnum.E
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.KtEnum.E

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.KtEnum.valueOf("E")
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.KtEnum.valueOf("E")

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.KtEnum::valueOf
}

fun testKotlinCallableReferences() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Generic::M
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Generic::S
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Generic::M
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Generic::S
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Generic<Int>::M
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Generic<Int>::S
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Generic<Int>::M
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Generic<Int>::S

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.NonGeneric::M
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.NonGeneric::S
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.NonGeneric::M
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.NonGeneric::S
}

fun testObjects() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Obj
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Obj

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Obj.M()
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Obj.M()

    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Obj::M
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Obj::M
}

fun testNestedObjects() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.ClassWithNestedObjects.Obj
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.ClassWithNestedObjects.Obj
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.ClassWithNestedObjects.WithCompanion
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.ClassWithNestedObjects.WithCompanion
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.ObjectWithNestedObjects.Obj
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.ObjectWithNestedObjects.Obj
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.ObjectWithNestedObjects.WithCompanion
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.ObjectWithNestedObjects.WithCompanion
}

fun testGetClass() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Obj::class
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Obj::class
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.Generic::class
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.Generic::class
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.NonGeneric::class
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.NonGeneric::class
}

fun testThreeParts() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.part3.Function()
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.part3.Variable
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.part3.Obj
}

fun testInner() {
    <!UNRESOLVED_REFERENCE!>part1<!><Int>.part2.WithInner.Inner::M
    part1.<!UNRESOLVED_REFERENCE!>part2<!><Int>.WithInner.Inner::M
}

fun testAbracadabra() {
    <!UNRESOLVED_REFERENCE!>part1<!><<!UNRESOLVED_REFERENCE!>Abracadabra<!>>.part2.part3.Function()
    part1.<!UNRESOLVED_REFERENCE!>part2<!><<!UNRESOLVED_REFERENCE!>Abracadabra<!>>.part3.Obj
    part1.part2.<!UNRESOLVED_REFERENCE!>part3<!><<!UNRESOLVED_REFERENCE!>Abracadabra<!>>.Variable
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, companionObject, enumDeclaration, enumEntry,
functionDeclaration, inner, nestedClass, nullableType, objectDeclaration, propertyDeclaration, typeParameter */
