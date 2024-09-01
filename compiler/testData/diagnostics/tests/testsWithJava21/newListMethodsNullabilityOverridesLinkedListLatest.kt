// DIAGNOSTICS: -JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE
// accidentally reported (K1 only) on x.addFirst/addLast/removeFirst/removeLast
// ISSUE: KT-68193
// ISSUE: KT-67804

import java.util.LinkedList

abstract class A1<E5> : LinkedList<E5>() {
    override fun addFirst(element: E5) {}
    override fun addLast(element: E5) {}

    override fun removeFirst(): E5 = super.removeFirst()
    override fun removeLast(): E5 = super.removeLast()
}

abstract class A2<E6> : LinkedList<E6>()  {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun addFirst(element: E6?) {}
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun addLast(element: E6?) {}

    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun removeFirst(): E6? = super.removeFirst()
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun removeLast(): E6? = super.removeLast()
}

abstract class A3 : LinkedList<String>() {
    override fun addFirst(element: String) {}
    override fun addLast(element: String) {}

    override fun removeFirst(): String = super.removeFirst()
    override fun removeLast(): String = super.removeLast()
}

abstract class A4 : LinkedList<String>() {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun addFirst(element: String?) {}
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun addLast(element: String?) {}

    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun removeFirst(): String? = super.removeFirst()
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun removeLast(): String? = super.removeLast()
}
