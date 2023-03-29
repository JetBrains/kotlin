// TARGET_BACKEND: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57777

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
