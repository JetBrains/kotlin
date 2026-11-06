// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

data class Data(val p: context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>)

class Setter {
    var p: Int = 0
        set(value: context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>) {}
}

interface Base {
    fun foo(): context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>
}

class Derived : Base {
    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>foo<!>() = TODO()
}

typealias Alias = context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>

val aliased: Alias = TODO()

fun catchParameter() {
    try {} catch (e: context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>) {}
}

data class NullableData(val p: (context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>)?)

class NullableSetter {
    var p: Int = 0
        set(value: (context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>)?) {}
}

interface NullableBase {
    fun foo(): (context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>)?
}

class NullableDerived : NullableBase {
    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>foo<!>() = TODO()
}

typealias NullableAlias = (context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>)?

val nullableAliased: NullableAlias = TODO()

fun nullableCatchParameter() {
    try {} catch (e: (context(<!UNRESOLVED_REFERENCE!>Unresolved<!>)<!SYNTAX!><!>)?) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, interfaceDeclaration, localProperty,
override, primaryConstructor, propertyDeclaration, setter, tryExpression, typeAliasDeclaration */
