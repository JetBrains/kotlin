// FILE: Container.java

public class Container<E> {

    Wrapper<E> w = null;

    public Wrapper<E> getWrapper() {
        return null;
    }

    public void setWrapper(Wrapper<E> wrapper) {}

    public E getSimple() {
        return null;
    }

    public void setSimple(E e) {}
}

// FILE: Wrapper.java

public class Wrapper<W> {
}

// FILE: test.kt

fun foo(container: Container<*>, wrapper: Wrapper<String>) {
    container.w = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.setWrapper(<!ARGUMENT_TYPE_MISMATCH!>wrapper<!>)

    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>"123"<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>"123"<!>)
    container.simple = null
    container.setSimple(null)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>container.simple<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>container.getSimple()<!>)
    container.simple = null!!
}

fun bar(container: Container<String>, wrapper: Wrapper<String>) {
    container.wrapper = wrapper
    container.setWrapper(wrapper)

    container.simple = "123"
    container.setSimple("123")
}

fun baz(container: Container<Any>, wrapper: Wrapper<String>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.simple = "123"
    container.setSimple("123")
}

fun gau(container: Container<String>, wrapper: Wrapper<Any>, arg: Any) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>456<!>
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>arg<!>
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>O<!>
    if (arg is String) {
        container.simple = arg
    }
}

fun dif(container: Container<String>, wrapper: Wrapper<Int>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
}

object O

fun out(container: Container<out Any>, wrapper: Wrapper<String>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.setWrapper(<!ARGUMENT_TYPE_MISMATCH!>wrapper<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>O<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>O<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>456<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>456<!>)
}

val x = 456

fun inn(container: Container<in String>, wrapper: Wrapper<Any>, arg: Any) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.setWrapper(<!ARGUMENT_TYPE_MISMATCH!>wrapper<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>O<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>O<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>456<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>456<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>arg<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>arg<!>)
    if (arg is String) {
        container.simple = arg
        container.setSimple(arg)
    }
}

fun <T> generic(container: Container<out T>, wrapper: Wrapper<out T>, arg: T) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.setWrapper(<!ARGUMENT_TYPE_MISMATCH!>wrapper<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>O<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>O<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>456<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>456<!>)
    container.simple = <!ASSIGNMENT_TYPE_MISMATCH!>arg<!>
    container.setSimple(<!ARGUMENT_TYPE_MISMATCH!>arg<!>)
}
