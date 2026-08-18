// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88141
interface A {
    context(s: String)
    fun toString(): String = "B"

    fun String.<!EXTENSION_SHADOWED_BY_MEMBER!>hashCode<!>(): Int = 1
}

interface B : A
interface C : A

class Adapter : B, C

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class D<!>(adapter: Adapter) : B by adapter, C by adapter

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, javaType,
primaryConstructor, propertyDeclaration */
