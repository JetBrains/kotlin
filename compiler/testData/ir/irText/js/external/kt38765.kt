// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6
package events

external open class internal {
    open class EventEmitterP : internal {
    }

    open class EventEmitterS : internal {
        constructor(a: Any)
    }

    object NestedExternalObject : internal {}

    enum class NestedExternalEnum {
        A, B
    }

    interface NestedExternalInterface {}
}
