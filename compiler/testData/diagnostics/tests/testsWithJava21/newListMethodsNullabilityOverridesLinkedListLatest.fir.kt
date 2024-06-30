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
    <!NOTHING_TO_OVERRIDE!>override<!> fun addFirst(element: E6?) {}
    <!NOTHING_TO_OVERRIDE!>override<!> fun addLast(element: E6?) {}

    override fun removeFirst(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>E6?<!> = super.removeFirst()
    override fun removeLast(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>E6?<!> = super.removeLast()
}

abstract class A3 : LinkedList<String>() {
    override fun addFirst(element: String) {}
    override fun addLast(element: String) {}

    override fun removeFirst(): String = super.removeFirst()
    override fun removeLast(): String = super.removeLast()
}

abstract class A4 : LinkedList<String>() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun addFirst(element: String?) {}
    <!NOTHING_TO_OVERRIDE!>override<!> fun addLast(element: String?) {}

    override fun removeFirst(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = super.removeFirst()
    override fun removeLast(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = super.removeLast()
}
