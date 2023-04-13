// TARGET_BACKEND: JS_IR

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
