// RUN_PIPELINE_TILL: FRONTEND
interface A { fun f() }

open class P(val z: B)

class B : A {
    override fun f() {}
    class C : A by <!NO_THIS!>this<!> {}
    class D(val x : B = <!NO_THIS!>this<!>)
    class E : P(<!NO_THIS!>this<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, nestedClass,
override, primaryConstructor, propertyDeclaration, thisExpression */
