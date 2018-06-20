// PROBLEM: none

abstract class Base(var x: String)

sealed class Sealed(s: String) : Base(s)

<caret>class Derived : Sealed("123")