// RUN_PIPELINE_TILL: FRONTEND
// Added just to check that recursive parameter in Java doesn't provoke StackOverflow
// FILE: Recursive.java

public class Recursive {
    public <A extends A> void foo(A a) {}
}

// FILE: RecursiveDerived.java

public class RecursiveDerived extends Recursive {
    public <Y> void foo(Y a) {}
}

// FILE: use.kt

class Derived : RecursiveDerived() {
    override fun <T> foo (t: T) {}

    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS!>abstract<!> fun bar() // To provoke error thus avoiding later phases
}

/* GENERATED_FIR_TAGS: classDeclaration, javaType */
