// COMPILATION_ERRORS
// FILE: I1.kt
interface I1
// FILE: I2.kt
interface I2

// FILE: Foo.kt
typealias Alias<T> where T : I1, T : I2 = List<T>
