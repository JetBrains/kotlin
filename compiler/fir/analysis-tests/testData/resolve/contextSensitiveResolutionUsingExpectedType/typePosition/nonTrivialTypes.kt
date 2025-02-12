// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: JavaClass.java
public class JavaClass {
    public static MySealed foo() { return null; }
}

// FILE: main.kt
sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
}

interface MyGeneric<E1> {}

sealed interface MyGenericSealed<E2> : MyGeneric<E2> {
    class Left(val x: String): MyGenericSealed<String>
    class Right(val y: String): MyGenericSealed<String>
}

fun main(mg: MyGeneric<Any>, cs: CharSequence) {
    when (val foo = JavaClass.foo()) {
        is Left -> foo.x
        is Right -> foo.y
    }

    // mg: MyGenericSealed<String> & MyGeneric<Any> => MyGenericSealed <: MyGeneric => should work
    mg <!UNCHECKED_CAST!>as MyGenericSealed<String><!>
    when (mg) {
        is Left -> mg.x
        is Right -> mg.y
    }

    // cs: MyGenericSealed<String> & CharSequence => neither component is a subclass of another => should not work
    cs <!UNCHECKED_CAST!>as MyGenericSealed<String><!>
    <!NO_ELSE_IN_WHEN!>when<!> (cs) {
        is <!UNRESOLVED_REFERENCE!>Left<!> -> cs.<!UNRESOLVED_REFERENCE!>x<!>
        is <!UNRESOLVED_REFERENCE!>Right<!> -> cs.<!UNRESOLVED_REFERENCE!>y<!>
    }
}
