// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.
package foo

class A

fun A.get_bar() = 23

val A.bar: Int
  get() = 42
