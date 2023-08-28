
interface Inv
class Impl : Inv

class Scope<InterfaceT, ImplementationT : InterfaceT>(private val implClass: <!UNRESOLVED_REFERENCE!>j<!>.Class<ImplementationT>) {
    fun foo(c: Collection<InterfaceT>) {
        val hm = c.asSequence()
            .filter(implClass::<!UNRESOLVED_REFERENCE!>isInstance<!>)
            .map(implClass::<!UNRESOLVED_REFERENCE!>cast<!>)
            .<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>toSet<!>()
    }
}
