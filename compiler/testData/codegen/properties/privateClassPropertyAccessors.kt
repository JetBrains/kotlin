import kotlin.reflect.KProperty

class C {
    // All these properties should have corresponding accessors
    private val valWithGet: String
        get() = ""

    private var varWithGetSet: Int
        get() = 0
        set(value) {}

    private var delegated: Int by Delegate

    private var String.extension: String
        get() = this
        set(value) {}

    companion object {
        private val classObjectVal: Long
            get() = 1L
    }

    // This property should not have accessors
    private var varNoAccessors = 0L
        get set
}


object Delegate {
    operator fun getValue(x: C, p: KProperty<*>): Nothing = throw AssertionError()

    operator fun setValue(x: C, p: KProperty<*>, value: Int): Nothing = throw AssertionError()
}
