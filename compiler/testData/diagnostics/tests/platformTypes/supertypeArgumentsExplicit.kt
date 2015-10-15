// !EXPLICIT_FLEXIBLE_TYPES

interface A<T>
interface B<T>: A<ft<T, T?>>

interface C: A<String>, B<String>
interface D: B<String>, A<String>
interface E: A<String?>, B<String?>
interface F: A<String?>, B<String>

interface G: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A<String>, B<String?><!>
interface H: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A<Int>, B<String><!>
interface I: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>B<Int>, A<String><!>