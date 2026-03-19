// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-36111

// KT-36111: False negative warning on exhaustive `when` with flexible type of sealed class in subject

// FILE: Utils.java
public class Utils {
    public static E getEnum() {
        return null;
    }

    public static A getSealed() {
        return null;
    }
}

// FILE: kt36111.kt
enum class E {
    B, C
}

sealed class A
class B : A()
class C : A()

fun test() {
    val e = Utils.getEnum() // type: (E..E?)
    val s = when (e) { // Warning WHEN_ENUM_CAN_BE_NULL_IN_JAVA
        E.B -> ""
        E.C -> ""
    }
    s.length
}

fun test_2() {
    val e = Utils.getSealed() // type: (A..A?)
    val s = when (e) { // No warning - false negative (bug)
        is B -> ""
        is C -> ""
    }
    s.length
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, equalityExpression, flexibleType,
functionDeclaration, isExpression, javaFunction, localProperty, propertyDeclaration, sealed, smartcast, stringLiteral,
whenExpression, whenWithSubject */
