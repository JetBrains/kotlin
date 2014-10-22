// !EXPLICIT_FLEXIBLE_TYPES
// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND

trait A<T>
trait B<T>: A<ft<T, T?>>

trait C: A<String>, B<String>
trait D: B<String>, A<String>
trait E: A<String?>, B<String?>
trait F: A<String?>, B<String>

trait G: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A<String>, B<String?><!>
trait H: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A<Int>, B<String><!>
trait I: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>B<Int>, A<String><!>