// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: main.kt
class Inv<T>(val x: T)

class A<T : Inv<in T>> {
    fun foo(): T = null!!
}

class Inv2<T : Inv2<in T>>(val x: T)

fun main(a: A<*>, j: JavaClass<*>, i2: Inv2<*>) {
    // Probably it's too restrictive to suppose star projection type here as Any?,
    // but looks like we can refine it later
    a.foo() checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }
    j.foo() checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }
    i2.x checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any?>() }

    j.bar(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>Any()<!>)
    j.bar(null)
}

// FILE: JavaClass.java
public class JavaClass<T extends JavaClass<? super T>> {
    public void bar(T... x) {}
    public T foo() {}
}
