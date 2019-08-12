// !DIAGNOSTICS: -ABSTRACT_MEMBER_NOT_IMPLEMENTED

// Implementing kotlin functions isn't allowed on JS
class <!IMPLEMENTING_FUNCTION_INTERFACE!>A<!> : (Int) -> Int

// Array as upper bound isn't allowed on JVM
class B<<!UPPER_BOUND_CANNOT_BE_ARRAY!>T : Array<*><!>>