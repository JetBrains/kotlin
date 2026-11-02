// RUN_PIPELINE_TILL: FRONTEND
package a.b

annotation class Anno

class Outer {
    class Nested
}

fun incompleteType(vararg p: Int.<!SYNTAX!><!>) {}
fun incompleteQualifiedType(vararg p: a.b.<!SYNTAX!><!>) {}
fun incompleteNestedType(vararg p: Outer.Nested.<!SYNTAX!><!>) {}
fun incompleteUnresolvedQualifier(vararg p: Unresolved.<!SYNTAX!><!>) {}
fun incompleteUnresolvedNestedQualifier(vararg p: Outer.Unresolved.<!SYNTAX!><!>) {}
fun annotatedType(vararg p: <!WRONG_ANNOTATION_TARGET, WRONG_ANNOTATION_TARGET!>@Anno<!> Int) {}
fun annotatedUnresolvedType(vararg p: <!UNRESOLVED_REFERENCE!><!WRONG_ANNOTATION_TARGET, WRONG_ANNOTATION_TARGET!>@Anno<!> Unresolved<!>) {}
fun annotatedIncompleteTypeArgument(vararg p: List<<!WRONG_ANNOTATION_TARGET!>@Anno<!> Int.<!SYNTAX!>><!><!SYNTAX!><!>) {}

class Constructor(vararg p: Unresolved.<!SYNTAX!><!>)
class ConstructorProperty(vararg val p: Unresolved.<!SYNTAX!><!>)

fun localDeclarations() {
    fun local(vararg p: Unresolved.<!SYNTAX!><!>) {}
    val anonymous = fun(vararg p: Unresolved.<!SYNTAX!><!>) {}
}

fun nonVarargIncompleteType(p: Int.<!SYNTAX!><!>) {}
fun nonVarargIncompleteQualifiedType(p: a.<!UNRESOLVED_REFERENCE!>b<!>.<!SYNTAX!><!>) {}
fun nonVarargIncompleteNestedType(p: Outer.Nested.<!SYNTAX!><!>) {}
fun nonVarargIncompleteUnresolvedQualifier(p: <!UNRESOLVED_REFERENCE!>Unresolved<!>.<!SYNTAX!><!>) {}
fun nonVarargIncompleteUnresolvedNestedQualifier(p: Outer.<!UNRESOLVED_REFERENCE!>Unresolved<!>.<!SYNTAX!><!>) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, classDeclaration, functionDeclaration, localFunction,
localProperty, nestedClass, outProjection, primaryConstructor, propertyDeclaration, vararg */
