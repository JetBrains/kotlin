package foo

class Derived3Error : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Sealed1<!>() // should be an error
