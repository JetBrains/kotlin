// RUN_PIPELINE_TILL: FRONTEND
open class A
class B : <!SUPERTYPE_NOT_INITIALIZED!>A<!>

open class C(x: Int)
class D : <!NO_VALUE_FOR_PARAMETER, SUPERTYPE_NOT_INITIALIZED!>C<!>
class E : C(10)
class F() : C(10)

class <!CONFLICTING_OVERLOADS!>G<!> : <!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>C<!>(10) {
    <!CONFLICTING_OVERLOADS!>constructor()<!> : super(1)
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, secondaryConstructor */
