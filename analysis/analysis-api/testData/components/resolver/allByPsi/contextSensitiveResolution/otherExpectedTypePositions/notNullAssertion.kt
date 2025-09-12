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
    receive<MyClass>(NestedInheritor!!)
    receive<MyClass>(myClassProp!!)
    receive<MyClass>(stringProp!!)
    receive<MyClass>(getNestedInheritor()!!)
}

fun testIndexAccess(): Int? {
    val map = mapOf<MyClass, Int>(MyClass.myClassProp to 1,  MyClass.stringProp to 2, MyClass.NestedInheritor to 3, MyClass.getNestedInheritor() to 4)
    return map[myClassProp]
    return map[stringProp]
    return map[NestedInheritor]
    return map[getNestedInheritor()]
}

fun testCallableReference() {
    val i150: KProperty<MyClass> = ::myClassProp
}

operator fun MyClass.rangeTo(that: MyClass): ClosedFloatingPointRange<MyClass> {
    return null!!
}
operator fun MyClass.contains(that: MyClass): Boolean {
    return true
}

fun testOverriden(a: Int) {
    val a: MyClass = myClassProp
    a !in MyClass.myClassProp..NestedInheritor
}
