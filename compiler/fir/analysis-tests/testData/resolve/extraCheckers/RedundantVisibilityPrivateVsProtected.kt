// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82487

// FILE: JavaParent.java
public class JavaParent {
    protected void foo() {}
}

// FILE: test.kt
abstract class Abs {
    protected abstract fun foo()
}

class Owner {
    private object Obj : Abs() {
        <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override fun foo() {}
    }

    private object ObjJava : JavaParent() {
        <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override fun foo() {}
    }
}

private object Obj : Abs() {
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override fun foo() {}
}

private object ObjJava : JavaParent() {
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> override fun foo() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, nestedClass, objectDeclaration, override */
