// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

import kotlin.reflect.KProperty

open class MyClass {
    object NestedInheritor : MyClass()

    companion object {
        val myClassProp: MyClass = MyClass()
        val stringProp: String = ""
        fun getNestedInheritor() = NestedInheritor
    }
}

fun <T>receive(e: T) {}
val ClassMemberAlias = MyClass.NestedInheritor

fun testNotNullAssertion() {
    receive<MyClass>(NestedInheritor<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    receive<MyClass>(myClassProp<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    receive<MyClass>(<!ARGUMENT_TYPE_MISMATCH!>stringProp<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    receive<MyClass>(<!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()!!)
}

fun testIndexAccess(): Int? {
    val map = mapOf<MyClass, Int>(MyClass.myClassProp to 1,  <!ARGUMENT_TYPE_MISMATCH!>MyClass.stringProp to 2<!>, MyClass.NestedInheritor to 3, MyClass.getNestedInheritor() to 4)
    return map[myClassProp]
    return map[<!ARGUMENT_TYPE_MISMATCH!>stringProp<!>]
    return map[NestedInheritor]
    return map[<!UNRESOLVED_REFERENCE!>getNestedInheritor<!>()]
}

fun testCallableReference() {
    val i150: KProperty<MyClass> = ::<!UNRESOLVED_REFERENCE!>myClassProp<!>
}

operator fun MyClass.rangeTo(that: MyClass): ClosedFloatingPointRange<<!UPPER_BOUND_VIOLATED!>MyClass<!>> {
    return null!!
}
operator fun MyClass.contains(that: MyClass): Boolean {
    return true
}

fun testOverriden(a: Int) {
    val a: MyClass = myClassProp
    a !in MyClass.myClassProp..NestedInheritor
}