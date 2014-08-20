open class Generic<T>

trait A : Generic<String>

trait B : Generic<Int>

trait C : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A, B<!>
