// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: My.java

public class My {
    static public My create() { return new My(); }
    public void foo() {}
}

// FILE: Test.kt

fun test() {
    val my = My.create()
    if (my == null) {
        my<!UNSAFE_CALL!>.<!>foo()
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, ifExpression, javaFunction, localProperty,
nullableType, propertyDeclaration, smartcast */
