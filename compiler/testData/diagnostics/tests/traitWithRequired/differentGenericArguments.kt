open class Generic<T>

trait A : Generic<Int>

trait B : Generic<String>

class <!UNMET_TRAIT_REQUIREMENT, UNMET_TRAIT_REQUIREMENT!>Y<!> : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A, B<!>
class Z : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A, B, Generic<Int>()<!>
