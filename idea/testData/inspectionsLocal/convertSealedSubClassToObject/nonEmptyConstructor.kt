// PROBLEM: none

sealed class Sealed(val y: Int)

<caret>class SubSealed(x: Int) : Sealed(x)