import java.util.*

// InitializerTypeMismatchChecker reports this due JDK wasn't loaded

val x: SortedSet<Int> = <!INITIALIZER_TYPE_MISMATCH!>TreeSet()<!>
