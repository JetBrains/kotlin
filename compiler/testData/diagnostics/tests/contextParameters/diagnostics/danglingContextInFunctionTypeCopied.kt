// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

data class Data(val p: context(Unresolved)<!SYNTAX!><!>)

class Setter {
    var p: Int = 0
        set(value: context(Unresolved)<!SYNTAX!><!>) {}
}

interface Base {
    fun foo(): context(Unresolved)<!SYNTAX!><!>
}

class Derived : Base {
    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>foo<!>() = TODO()
}

typealias Alias = context(Unresolved)<!SYNTAX!><!>

val aliased: Alias = TODO()

fun catchParameter() {
    try {} catch (e: context(Unresolved)<!SYNTAX!><!>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, interfaceDeclaration, localProperty,
override, primaryConstructor, propertyDeclaration, setter, tryExpression, typeAliasDeclaration */
