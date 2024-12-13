// RUN_PIPELINE_TILL: BACKEND
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
    context(n: ProtectedClass)
    public var privateInOpenProperty: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }

    context(n: ProtectedClass)
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

context(w: PublicClass, x: PrivateClass, z: InternalClass)
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

context(w: PublicClass, x: PrivateClass, z: InternalClass)
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
    context(n: ProtectedClass)
    public var publicInChild: String
        get() {
            n.a
            return ""
        }
        set(value) {
            n.a
        }

    context(n: ProtectedClass)
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


context(w: PublicClass, x: PrivateClass, z: InternalClass)
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

context(w: PublicClass, x: PrivateClass, z: InternalClass)
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
