// !LANGUAGE: +NewInference

interface Inv
class Impl : Inv

class Scope<InterfaceT, ImplementationT : InterfaceT>(private val implClass: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>j.Class<ImplementationT><!>) {
    fun foo(c: Collection<InterfaceT>) {
        val hm = c.asSequence()
            .<!INAPPLICABLE_CANDIDATE!>filter<!>(<!UNRESOLVED_REFERENCE!>implClass::isInstance<!>)
            .<!INAPPLICABLE_CANDIDATE!>map<!>(<!UNRESOLVED_REFERENCE!>implClass::cast<!>)
            .toSet()
    }
}
