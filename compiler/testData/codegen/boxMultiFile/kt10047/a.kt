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