// FIR_DUMP
<!REDUNDANT_MODIFIER_FOR_TARGET!>open<!> interface OpenInterface {
    fun <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS!>defaultModalityFuncWithoutBody<!>()
    fun defaultModalityFuncWithBody() {}
    <!ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS!>abstract<!> fun abstractFunc()
    open fun openFunc() {}

    val <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>defaultModalityValWithoutGetter<!>: Any
    val defaultModalityValWithGetter: Any get() = 42
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val abstractVal: Any
    open val openVal: Any get() = 42

    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val abstractValWithGetter: Any get() = 42
    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> var abstractValWithSetter: Any set(value) {}
}
