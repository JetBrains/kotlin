// RUN_PIPELINE_TILL: FRONTEND
interface A { fun f() }

open class P(val z: B)

class B : A {
    override fun f() {}
    class C : A by <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!> {}
    class D(val x : B = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>)
    class E : P(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, nestedClass,
override, primaryConstructor, propertyDeclaration, thisExpression */
