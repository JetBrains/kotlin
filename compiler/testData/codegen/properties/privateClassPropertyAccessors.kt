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

    default object {
        private val classObjectVal: Long
            get() = 1L
    }

    // This property should not have accessors
    private var varNoAccessors = 0L
        get set
}


object Delegate {
    fun get(x: C, p: PropertyMetadata) = throw AssertionError()

    fun set(x: C, p: PropertyMetadata, value: Int) = throw AssertionError()
}
