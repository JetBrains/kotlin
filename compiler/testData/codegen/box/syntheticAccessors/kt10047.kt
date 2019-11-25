// IGNORE_BACKEND_FIR: JVM_IR
// FILE: a.kt

package test2

import test.Actor
import test.O2dScriptAction

class CompositeActor : Actor()

public open class O2dDialog : O2dScriptAction<CompositeActor>() {

    fun test() = { owner }()

    fun test2() = { calc() }()
}

fun box(): String {
    if (O2dDialog().test() != null) return "fail 1"
    if (O2dDialog().test2() != null) return "fail 2"

    return "OK"
}

// FILE: b.kt

package test

open class Actor

abstract public class O2dScriptAction<T : Actor> {
    protected var owner: T? = null
        private set

    protected fun calc(): T? = null

}
