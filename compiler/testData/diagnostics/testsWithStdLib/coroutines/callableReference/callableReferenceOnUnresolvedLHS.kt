// !LANGUAGE: +NewInference

interface Inv
class Impl : Inv

class Scope<InterfaceT, ImplementationT : InterfaceT>(private val implClass: <!UNRESOLVED_REFERENCE!>j<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>Class<!><ImplementationT>) {
    fun foo(c: Collection<InterfaceT>) {
        val <!UNUSED_VARIABLE!>hm<!> = c.asSequence()
            .filter(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>implClass<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>isInstance<!>)
            .<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>map<!>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>implClass<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>cast<!>)
            .<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>toSet<!>()
    }
}
