// FILE: I1.kt
interface I1
// FILE: I2.kt
interface I2

// FILE: Foo.kt
val <T> T.foo: String? where T : I1, T : I2 get() = null
