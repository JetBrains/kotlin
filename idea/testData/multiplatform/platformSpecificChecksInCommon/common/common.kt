// !DIAGNOSTICS: -ABSTRACT_MEMBER_NOT_IMPLEMENTED

// Implementing kotlin functions isn't allowed on JS
class A : (Int) -> Int

// Array as upper bound isn't allowed on JVM
class B<T : Array<*>>