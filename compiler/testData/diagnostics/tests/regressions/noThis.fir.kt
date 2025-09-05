// RUN_PIPELINE_TILL: FRONTEND
interface A { fun f() }

open class P(val z: B)

class B : A {
    override fun f() {}
    class C : A by <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>this<!> {}
    class D(val x : B = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>this<!>)
    class E : P(<!INACCESSIBLE_OUTER_CLASS_RECEIVER!>this<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, nestedClass,
override, primaryConstructor, propertyDeclaration, thisExpression */
