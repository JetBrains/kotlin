// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// WITH_STDLIB

sealed class Parent {
    companion object {
        val bar: Parent.MyClass get() = MyClass()
    }

    class MyClass : Parent() {
        companion object
    }
}

val Any.bar: String get() = ""

fun foo(a: Parent.MyClass) {}

fun f1(): Parent.MyClass = <!UNRESOLVED_REFERENCE!>bar<!>
fun f2(m: Parent.MyClass) = when (m) { <!UNRESOLVED_REFERENCE!>bar<!> -> 1; else -> 2 }
fun f3(m: Parent.MyClass) = m == <!UNRESOLVED_REFERENCE!>bar<!>
fun f4() { var v = Parent.MyClass(); v = <!UNRESOLVED_REFERENCE!>bar<!> }
fun f5(c: Boolean) = foo(if (c) <!UNRESOLVED_REFERENCE!>bar<!> else Parent.MyClass())
fun f6(a: Parent.MyClass = <!UNRESOLVED_REFERENCE!>bar<!>) {}
fun f7(a: Parent.MyClass?) {}
fun t7() = f7(<!UNRESOLVED_REFERENCE!>bar<!>)
val l: List<Parent.MyClass> = listOf(<!UNRESOLVED_REFERENCE!>bar<!>)
class H { var m = Parent.MyClass() }
fun t9(h: H) { h.m = <!UNRESOLVED_REFERENCE!>bar<!> }
fun f10(vararg a: Parent.MyClass) {}
fun t10() = f10(<!UNRESOLVED_REFERENCE!>bar<!>)
typealias TA = Parent.MyClass
fun f11(a: TA) {}
fun t11() = f11(<!UNRESOLVED_REFERENCE!>bar<!>)
fun <T : <!FINAL_UPPER_BOUND!>Parent.MyClass<!>> g(t: T) {}
fun t12() = <!CANNOT_INFER_PARAMETER_TYPE!>g<!>(<!UNRESOLVED_REFERENCE!>bar<!>)

/* GENERATED_FIR_TAGS: assignment, classDeclaration, companionObject, equalityExpression, functionDeclaration, getter,
ifExpression, integerLiteral, localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration,
propertyWithExtensionReceiver, sealed, typeAliasDeclaration, typeConstraint, typeParameter, vararg, whenExpression,
whenWithSubject */
