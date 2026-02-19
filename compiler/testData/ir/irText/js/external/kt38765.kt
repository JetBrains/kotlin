// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

package events

external open class internal {
    fun function(): String
    var property: Int

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

external interface A {
    companion object {
    }
}
