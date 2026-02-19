// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80864
// FILE: Foo.java

public class Foo {
    public final int foo;

    public int bar;

    public Foo() {
        this.foo = 0;
        this.bar = 0;
    }
}

// FILE: main.kt

fun main() {
    val foo = Foo()
    <!VAL_REASSIGNMENT!>foo.foo<!> = 1
    foo.bar = 1
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, javaFunction, javaProperty, javaType,
localProperty, propertyDeclaration */
