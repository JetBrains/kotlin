// !MARK_DYNAMIC_CALLS

fun withDynamicReceiver(d: dynamic.() -> Unit) {}

fun test() = withDynamicReceiver {
    foo
    bar = 1
}

fun test2() = withDynamicReceiver(fun dynamic.() {})

val dynamicProperty: dynamic.() -> Unit = TODO()

fun test(d: dynamic, dynamicParameter: dynamic.() -> Unit) {
    d.dynamicProperty()
    d.dynamicParameter()
}
