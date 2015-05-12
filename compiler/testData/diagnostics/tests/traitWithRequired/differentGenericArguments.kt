open class Generic<T>

interface A : <!TRAIT_WITH_SUPERCLASS!>Generic<Int><!>

interface B : <!TRAIT_WITH_SUPERCLASS!>Generic<String><!>

<!UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT!>class Y<!> : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A, B<!>
class Z : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A, B, Generic<Int>()<!>
