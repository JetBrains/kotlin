// ISSUE: KT-83652, KT-84154
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS

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
    part1<Int>.part2.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GenericJava<!>::S
    part1<Int>.part2.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GenericJava<!>::M
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GenericJava<!><!>::S
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GenericJava<!><!>::M
    part1<Int>.part2.GenericJava<Int>::S
    part1<Int>.part2.GenericJava<Int>::M
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.GenericJava<Int><!>::S
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.GenericJava<Int><!>::M

    // non-generic:
    part1<Int>.part2.NonGenericJava::S
    part1<Int>.part2.NonGenericJava::M
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.NonGenericJava<!>::S
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.NonGenericJava<!>::M
}

fun testJavaStatics() {
    part1<Int>.part2.GenericJava.S()
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.GenericJava<!>.S()
    part1<Int>.part2.GenericJava<Int>.S()
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.GenericJava<Int><!>.S()

    part1<Int>.part2.NonGenericJava.S()
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.NonGenericJava<!>.S()
}

fun testKotlinCompanions() {
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1<Int>.part2<!>.Generic<!>
    part1.part2<Int>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Generic<!>

    part1<Int>.part2.Generic.toString()
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.Generic<!>.toString()

    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1<Int>.part2<!>.NonGeneric<!>
    part1.part2<Int>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NonGeneric<!>

    part1<Int>.part2.NonGeneric.toString()
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.NonGeneric<!>.toString()
}

fun testEnums() {
    part1<Int>.part2.KtEnum.E
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.KtEnum<!>.E

    part1<Int>.part2.KtEnum.valueOf("E")
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.KtEnum<!>.valueOf("E")

    part1<Int>.part2.KtEnum::valueOf
}

fun testKotlinCallableReferences() {
    part1<Int>.part2.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Generic<!>::M
    part1<Int>.part2.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Generic<!>::S
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Generic<!><!>::M
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Generic<!><!>::S
    part1<Int>.part2.Generic<Int>::M
    part1<Int>.part2.Generic<Int>::<!UNRESOLVED_REFERENCE!>S<!>
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.Generic<Int><!>::M
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.Generic<Int><!>::<!UNRESOLVED_REFERENCE!>S<!>

    part1<Int>.part2.NonGeneric::M
    part1<Int>.part2.NonGeneric::S
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.NonGeneric<!>::M
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.NonGeneric<!>::S
}

fun testObjects() {
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1<Int>.part2<!>.Obj<!>
    part1.part2<Int>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Obj<!>

    part1<Int>.part2.Obj.M()
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.Obj<!>.M()

    part1<Int>.part2.Obj::M
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.Obj<!>::M
}

fun testNestedObjects() {
    part1<Int>.part2.ClassWithNestedObjects.Obj
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.ClassWithNestedObjects<!>.Obj<!>
    part1<Int>.part2.ClassWithNestedObjects.WithCompanion
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.ClassWithNestedObjects<!>.WithCompanion<!>
    part1<Int>.part2.ObjectWithNestedObjects.Obj
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.ObjectWithNestedObjects<!>.Obj<!>
    part1<Int>.part2.ObjectWithNestedObjects.WithCompanion
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.ObjectWithNestedObjects<!>.WithCompanion<!>
}

fun testGetClass() {
    part1<Int>.part2.Obj::class
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.Obj<!>::class
    part1<Int>.part2.Generic::class
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.Generic<!>::class
    part1<Int>.part2.NonGeneric::class
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.NonGeneric<!>::class
}

fun testThreeParts() {
    part1<Int>.part2.part3.Function()
    part1<Int>.part2.part3.Variable
    part1<Int>.part2.part3.Obj
}

fun testInner() {
    part1<Int>.part2.WithInner.Inner::M
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Int>.WithInner<!>.Inner::M
}

fun testAbracadabra() {
    part1<Abracadabra>.part2.part3.Function()
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>part1.part2<Abracadabra>.part3<!>.Obj<!>
    <!TYPE_ARGUMENTS_NOT_ALLOWED!>part1.part2.part3<<!UNRESOLVED_REFERENCE!>Abracadabra<!>><!>.Variable
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, companionObject, enumDeclaration, enumEntry,
functionDeclaration, inner, nestedClass, nullableType, objectDeclaration, propertyDeclaration, typeParameter */
