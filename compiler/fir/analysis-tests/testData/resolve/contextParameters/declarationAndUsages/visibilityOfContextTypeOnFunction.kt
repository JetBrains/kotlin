// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-73716

public class PublicClass {
    val a: String = ""
}
private class PrivateClass {
    val a: String = ""
}
open class OpenClass {
    protected class ProtectedClass {
        val a: String = ""
    }
    context(<!EXPOSED_PARAMETER_TYPE!>a: ProtectedClass<!>)
    public fun test1() {
        a.a
    }
    context(<!EXPOSED_PARAMETER_TYPE!>a: ProtectedClass<!>)
    internal fun test2() {
        a.a
    }
    context(a: ProtectedClass)
    protected fun test3() {
        a.a
    }
    context(a: ProtectedClass)
    private fun test4() {
        a.a
    }
}
internal class InternalClass {
    val a: String = ""
}


context(w: PublicClass, <!EXPOSED_PARAMETER_TYPE!>x: PrivateClass<!>, <!EXPOSED_PARAMETER_TYPE!>y: OpenClass.<!INVISIBLE_REFERENCE!>ProtectedClass<!><!>, <!EXPOSED_PARAMETER_TYPE!>z: InternalClass<!>)
public fun test1() {
    w.a
    x.a
    y.<!INVISIBLE_REFERENCE!>a<!>
    z.a
}

context(w: PublicClass, <!EXPOSED_PARAMETER_TYPE!>x: PrivateClass<!>, <!EXPOSED_PARAMETER_TYPE!>y: OpenClass.<!INVISIBLE_REFERENCE!>ProtectedClass<!><!>, z: InternalClass)
internal fun test2() {
    w.a
    x.a
    y.<!INVISIBLE_REFERENCE!>a<!>
    z.a
}

context(w: PublicClass, x: PrivateClass, y: OpenClass.<!INVISIBLE_REFERENCE!>ProtectedClass<!>, z: InternalClass)
private fun test3() {
    w.a
    x.a
    y.<!INVISIBLE_REFERENCE!>a<!>
    z.a
}

class ProtectedChild: OpenClass() {
    context(y: OpenClass.ProtectedClass)
    private fun foo1() {
        y.a
    }
    context(<!EXPOSED_PARAMETER_TYPE!>y: OpenClass.ProtectedClass<!>)
    public fun foo2() {
        y.a
    }
    context(y: OpenClass.ProtectedClass)
    protected fun foo3() {
        y.a
    }
    context(<!EXPOSED_PARAMETER_TYPE!>y: OpenClass.ProtectedClass<!>)
    internal fun foo4() {
        y.a
    }
}
