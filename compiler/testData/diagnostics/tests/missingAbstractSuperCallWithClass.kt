// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76365

interface Foo {
    fun check(): String = "OK"
}
abstract class Base {
    abstract fun check(): String
}
abstract class Derived : Base(), Foo

class Derived2 : Derived() {
    override fun check(): String {
        super<Derived>.<!ABSTRACT_SUPER_CALL!>check<!>()
        return super.<!ABSTRACT_SUPER_CALL!>check<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, override, stringLiteral,
superExpression */
