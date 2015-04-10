open class Generic<T>

trait A : <!TRAIT_WITH_SUPERCLASS!>Generic<String><!>

trait B : <!TRAIT_WITH_SUPERCLASS!>Generic<Int><!>

trait C : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A, B<!>
