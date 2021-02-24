// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// see https://youtrack.jetbrains.com/issue/KT-21515

abstract class DerivedAbstract : C.Base() {
    open class Data
}

public class C {

    open class Base ()

    class Foo : <!UNRESOLVED_REFERENCE!>Data<!>()

    companion object : DerivedAbstract()
}
