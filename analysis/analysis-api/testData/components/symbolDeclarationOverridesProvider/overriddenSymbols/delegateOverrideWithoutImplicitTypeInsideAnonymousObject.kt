class MyClass {
    lateinit var d: IntermediateClass<Int>
    val prop = object : IntermediateClass<Int> by d {
        override fun isSc<caret>hemeFile(name: CharSequence): Boolean = name != "str"
    }
}

interface IntermediateClass<SCHEME : Number> : BaseClass<SCHEME, Int> {
}

interface BaseClass<SCHEME : Number, MUTABLE_SCHEME> {
    fun isSchemeFile(name: CharSequence): Boolean = true
}
