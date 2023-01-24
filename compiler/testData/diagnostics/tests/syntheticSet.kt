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
    <!TYPE_MISMATCH_WARNING!>container.w = wrapper<!>
    <!SYNTHETIC_SETTER_PROJECTED_OUT!>container.wrapper<!> = wrapper
    container.setWrapper(<!TYPE_MISMATCH!>wrapper<!>)

    container.simple = "123"
    container.setSimple(<!TYPE_MISMATCH!>"123"<!>)
}

fun bar(container: Container<String>, wrapper: Wrapper<String>) {
    container.wrapper = wrapper
    container.setWrapper(wrapper)

    container.simple = "123"
    container.setSimple("123")
}

fun baz(container: Container<Any>, wrapper: Wrapper<String>) {
    container.wrapper = <!TYPE_MISMATCH!>wrapper<!>
    container.simple = "123"
    container.setSimple("123")
}

fun gau(container: Container<String>, wrapper: Wrapper<Any>) {
    container.wrapper = <!TYPE_MISMATCH!>wrapper<!>
    container.simple = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>456<!>
}

fun dif(container: Container<String>, wrapper: Wrapper<Int>) {
    container.wrapper = <!TYPE_MISMATCH!>wrapper<!>
}

fun out(container: Container<out Any>, wrapper: Wrapper<String>) {
    <!SYNTHETIC_SETTER_PROJECTED_OUT!>container.wrapper<!> = wrapper
    container.setWrapper(<!TYPE_MISMATCH!>wrapper<!>)
    container.simple = "123"
    container.setSimple(<!TYPE_MISMATCH!>"123"<!>)
}

fun inn(container: Container<in String>, wrapper: Wrapper<Any>) {
    <!SYNTHETIC_SETTER_PROJECTED_OUT!>container.wrapper<!> = wrapper
    container.setWrapper(<!TYPE_MISMATCH!>wrapper<!>)
    container.simple = 456
    container.setSimple(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>456<!>)
}

fun <T> generic(container: Container<out T>, wrapper: Wrapper<out T>, arg: T) {
    <!SYNTHETIC_SETTER_PROJECTED_OUT!>container.wrapper<!> = wrapper
    container.setWrapper(<!TYPE_MISMATCH!>wrapper<!>)
    container.simple = arg
    container.setSimple(<!TYPE_MISMATCH!>arg<!>)
}
