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
    context(<!EXPOSED_PARAMETER_TYPE!>n: ProtectedClass<!>)
    public var privateInOpenProperty: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }

    context(<!EXPOSED_PARAMETER_TYPE!>n: ProtectedClass<!>)
    internal var internalInOpen: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }

    context(n: ProtectedClass)
    private var privateInOpen: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }

    context(n: ProtectedClass)
    protected var d: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }
}
internal class InternalClass {
    val a: String = ""
}

context(w: PublicClass, <!EXPOSED_PARAMETER_TYPE!>x: PrivateClass<!>, <!EXPOSED_PARAMETER_TYPE!>z: InternalClass<!>)
public var publicProperty: String
    get() {
        w.a
        x.a
        z.a
        return ""
    }
    set(value) {
        w.a
        x.a
        z.a
    }

context(w: PublicClass, x: PrivateClass, z: InternalClass)
private var privateProperty: String
    get() {
        w.a
        x.a
        z.a
        return ""
    }
    set(value) {
        w.a
        x.a
        z.a
    }

context(w: PublicClass, <!EXPOSED_PARAMETER_TYPE!>x: PrivateClass<!>, z: InternalClass)
internal var internalProperty: String
    get() {
        w.a
        x.a
        z.a
        return ""
    }
    set(value) {
        w.a
        x.a
        z.a
    }

class YChild : OpenClass() {
    context(<!EXPOSED_PARAMETER_TYPE!>n: ProtectedClass<!>)
    public var publicInChild: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }

    context(<!EXPOSED_PARAMETER_TYPE!>n: ProtectedClass<!>)
    internal var internalInChild: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }

    context(n: ProtectedClass)
    private var privateInChild: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }

    context(n: ProtectedClass)
    protected var protectedInChild: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }
}


context(w: PublicClass, <!EXPOSED_PARAMETER_TYPE!>x: PrivateClass<!>, <!EXPOSED_PARAMETER_TYPE!>z: InternalClass<!>)
var privateSetter: String
    public get() {
        w.a
        x.a
        z.a
        return ""
    }
    private set(value) {
        w.a
        x.a
        z.a
    }

context(w: PublicClass, <!EXPOSED_PARAMETER_TYPE!>x: PrivateClass<!>, <!EXPOSED_PARAMETER_TYPE!>z: InternalClass<!>)
var internalSetter: String
    public get() {
        w.a
        x.a
        z.a
        return ""
    }
    internal set(value) {
        w.a
        x.a
        z.a
    }
