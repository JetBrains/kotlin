open class Generic<T>

interface A : <!TRAIT_WITH_SUPERCLASS!>Generic<String><!>

interface B : <!TRAIT_WITH_SUPERCLASS!>Generic<Int><!>

interface C : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A, B<!>
