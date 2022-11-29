// FILE: Container.java

public class Container<E> {

    Wrapper<E> w = null;

    public Wrapper<E> getWrapper() {
        return null;
    }

    public void setWrapper(Wrapper<E> wrapper) {}
}

// FILE: Wrapper.java

public class Wrapper<W> {
}

// FILE: test.kt

fun foo(container: Container<*>, wrapper: Wrapper<String>) {
    container.w = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.setWrapper(<!ARGUMENT_TYPE_MISMATCH!>wrapper<!>)
}

fun bar(container: Container<String>, wrapper: Wrapper<String>) {
    container.wrapper = wrapper
    container.setWrapper(wrapper)
}

fun baz(container: Container<Any>, wrapper: Wrapper<String>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
}

fun gau(container: Container<String>, wrapper: Wrapper<Any>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
}

fun dif(container: Container<String>, wrapper: Wrapper<Int>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
}

fun out(container: Container<out Any>, wrapper: Wrapper<String>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.setWrapper(<!ARGUMENT_TYPE_MISMATCH!>wrapper<!>)
}

fun inn(container: Container<in String>, wrapper: Wrapper<Any>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.setWrapper(<!ARGUMENT_TYPE_MISMATCH!>wrapper<!>)
}

fun <T> generic(container: Container<out T>, wrapper: Wrapper<out T>) {
    container.wrapper = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
    container.setWrapper(<!ARGUMENT_TYPE_MISMATCH!>wrapper<!>)
}
