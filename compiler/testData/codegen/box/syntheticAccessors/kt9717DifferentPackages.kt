// IGNORE_BACKEND_FIR: JVM_IR
// FILE: a.kt

package a

import b.*

fun box(): String {
    BB().ok()
    return BB().OK
}

// FILE: b.kt

package b

public open class B {
    public var OK: String = "OK"
        protected set
}

public class BB : B() {
    public fun ok(): String = OK
}
