// RUN_PIPELINE_TILL: FRONTEND

// ---------------------- AssertJ declarations --------------------------
// FILE: AbstractComparableAssert.java
public abstract class AbstractComparableAssert<S extends AbstractComparableAssert<S, A>, A extends Comparable<? super A>> {
    public S isGreaterThan(A other) {
        return null;
    }
}

// FILE: ObjectAssert.java
public class ObjectAssert {
    public void inObject() {}
}

// FILE: Assertions.java
public class Assertions {
    public static ObjectAssert assertThat(Object actual) {
        return null;
    }

    public static <T extends Comparable<? super T>> AbstractComparableAssert<?,T> assertThat(T actual) {
        return null;
    }
}
// ---------------------- AssertJ declarations end --------------------------


// FILE: test.kt

abstract class MyComparableClass : Comparable<MyComparableClass>

fun test(a: MyComparableClass?) {
    <!TYPE_MISMATCH!>Assertions.<!UPPER_BOUND_VIOLATED!>assertThat<!>(a)<!>.<!UNRESOLVED_REFERENCE!>inObject<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaProperty, localProperty, propertyDeclaration,
starProjection, stringLiteral, whenExpression */
