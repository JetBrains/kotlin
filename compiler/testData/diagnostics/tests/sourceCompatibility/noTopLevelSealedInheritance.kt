// !LANGUAGE: -topLevelSealedInheritance

sealed class Base

class Derived : <!SEALED_SUPERTYPE!>Base<!>()
