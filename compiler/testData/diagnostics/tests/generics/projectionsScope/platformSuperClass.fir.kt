// !CHECK_TYPE

// FILE: Clazz.java
public class Clazz<T> {
    public T getT() { return null; }
    public Clazz<? super T> getSuperClass() { return null; }
}

// FILE: main.kt
fun test(clazz: Clazz<*>) {
    clazz.t checkType { _<Any?>() }
    clazz.getSuperClass() checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Clazz<*>?>() }
    clazz.getSuperClass().t checkType { _<Any?>() }

    clazz.superClass checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Clazz<*>?>() }
    clazz.superClass.t checkType { _<Any?>() }

    // See KT-9294
    if (clazz.superClass == null) {
        throw NullPointerException()
    }
}
