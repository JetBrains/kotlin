// !DIAGNOSTICS: -ABSTRACT_MEMBER_NOT_IMPLEMENTED

// No JS-specific errors
class A : (Int) -> Int
fun `name in backticks`() {}

// Array as upper bound isn't allowed on JVM
class B<<!UPPER_BOUND_CANNOT_BE_ARRAY!>T : Array<*><!>>