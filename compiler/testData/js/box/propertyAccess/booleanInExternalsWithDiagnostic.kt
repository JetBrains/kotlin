// TARGET_BACKEND: JS_IR
// SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: EXCEPTION

@JsName("Error")
open external class JsError(message: String) : Throwable

fun box(): String {
    val interfaceWithBoolean: InterfaceWithBoolean = js("{}")
    try {
        C().c = interfaceWithBoolean.foo
    } catch (e: JsError) {
        if (e.message.asDynamic().includes("Boolean expected for")) {
            return "OK"
        }
    }

    return "fail"
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
}