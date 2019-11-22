// !DIAGNOSTICS: -ABSTRACT_MEMBER_NOT_IMPLEMENTED

// No JS-specific errors
class A : (Int) -> Int
fun `name in backticks`() {}

// Array as upper bound isn't allowed on JVM
class B<T : Array<*>>