sealed class Parent

class Child1(val field1: Int): Parent()

class Child2(val field2: Int): Parent()


fun foo(parent: Parent) = when(parent) {
    is val <!UNUSED_VARIABLE!>child<!>: Child1 -> <!DEBUG_INFO_SMARTCAST!>parent<!>.field1 + parent.<!UNRESOLVED_REFERENCE!>field2<!>
    !is val _ : Child2 -> 10
    is val <!UNUSED_VARIABLE!>child<!> -> <!DEBUG_INFO_SMARTCAST!>parent<!>.field2 + parent.<!UNRESOLVED_REFERENCE!>field1<!>
}

fun foo2(parent: Parent) = when(parent) {
    is val child: Child1 -> child.field1 + child.<!UNRESOLVED_REFERENCE!>field2<!>
    !is val _ : Child2 -> 10
    is val child -> <!DEBUG_INFO_SMARTCAST!>child<!>.field2 + child.<!UNRESOLVED_REFERENCE!>field1<!>
}