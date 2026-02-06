// SAFE_EXTERNAL_BOOLEAN

fun box(): String {
    val interfaceWithBoolean: InterfaceWithBoolean = js("{}")
    C().c = interfaceWithBoolean.foo
    C().c = interfaceWithBoolean.bar

    return "OK"
}

abstract class A<T> {
    open fun get(): T {
        return this.asDynamic()["attr"].unsafeCast<T>()
    }

    open fun set(value: T) {
        this.asDynamic()["attr"] = value
    }
}

class B : A<Boolean>() {
    override fun set(value: Boolean) {
        if (value) {
            this.asDynamic()["attr"] = value
        }
    }
}

val b: A<Boolean> = B()

class C {
    var c: Boolean
        get() = b.get()
        set(newValue) {
            b.set(newValue)
        }
}

external interface InterfaceWithBoolean {
    var foo: Boolean
    @JsName("goo")
    var bar: Boolean
}