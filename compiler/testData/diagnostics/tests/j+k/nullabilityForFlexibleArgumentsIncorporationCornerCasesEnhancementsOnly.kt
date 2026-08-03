// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnhancementsOfSecondIncorporationKind25 -EliminateSecondKindIncorporation

// FILE: JClass.java
public class JClass {
    public static <Y, X extends Inv<? super Y>> X foo(Y y, X x) { return null; }

    public static <T extends In<? super T>> String withIn(T actual) {
        return "";
    }

    public static <T> int withIn(T actual) {
        return 1;
    }

    public static <T extends Inv<? super T>> String withInv(T actual) {
        return "";
    }

    public static <T> int withInv(T actual) {
        return 1;
    }
}

// FILE: main.kt
class A
interface In<in E>
interface Inv<E>

abstract class MyIn : In<MyIn>
abstract class MyInv : Inv<MyInv>

fun main(x: MyIn?, y: MyInv?, aInv: Inv<A>, a: A?) {
    <!TYPE_MISMATCH!>JClass.<!UPPER_BOUND_VIOLATED!>foo<!>(a, aInv)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>JClass.withIn(x)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)"), TYPE_MISMATCH!>JClass.<!UPPER_BOUND_VIOLATED!>withInv<!>(y)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, in, interfaceDeclaration, javaFunction,
nullableType, typeParameter */
