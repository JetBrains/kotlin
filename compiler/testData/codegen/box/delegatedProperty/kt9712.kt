// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

import kotlin.properties.Delegates

object X {
    public var O: String
        by Delegates.observable("O") { prop, old, new -> }
        private set
}

open class A {
    public var K: String
        by Delegates.observable("") { prop, old, new -> }
        protected set
}

class B : A() {
    init {
        K = "K"
    }
}

fun box(): String =
        X.O + B().K
