// !LANGUAGE: +NewInference

interface Inv
class Impl : Inv

class Scope<InterfaceT, ImplementationT : InterfaceT>(private val implClass: <!UNRESOLVED_REFERENCE!>j.Class<ImplementationT><!>) {
    fun foo(c: Collection<InterfaceT>) {
        val hm = c.asSequence()
            .<!INAPPLICABLE_CANDIDATE!>filter<!>(<!UNRESOLVED_REFERENCE!>implClass::isInstance<!>)
            .<!INAPPLICABLE_CANDIDATE!>map<!>(<!UNRESOLVED_REFERENCE!>implClass::cast<!>)
            .toSet()
    }
}
