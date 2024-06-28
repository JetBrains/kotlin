// FIR_IDENTICAL
// FIR_DUMP
<!REDUNDANT_MODIFIER_FOR_TARGET!>open<!> interface OpenInterface {
    fun defaultModalityFuncWithoutBody()
    fun defaultModalityFuncWithBody() {}
    abstract fun abstractFunc()
    open fun openFunc() {}

    val defaultModalityValWithoutGetter: Any
    val defaultModalityValWithGetter: Any get() = 42
    abstract val abstractVal: Any
    open val openVal: Any get() = 42

    abstract val abstractValWithGetter: Any <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = 42<!>
    abstract var abstractValWithSetter: Any <!ABSTRACT_PROPERTY_WITH_SETTER!>set(value) {}<!>
}
