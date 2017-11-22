// !WITH_NEW_INFERENCE
open class SuperOuter<E> {
    inner open class SuperInner<F>
}

class DerivedOuter<G> : SuperOuter<G>() {
    inner class DerivedInner<H> : SuperOuter<G>.<!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>SuperInner<!><H>()
}

fun bare(x: SuperOuter<*>.SuperInner<*>, y: Any?) {
    if (<!USELESS_IS_CHECK!>x is SuperOuter.SuperInner<!>) return
    if (y is <!NO_TYPE_ARGUMENTS_ON_RHS!>SuperOuter.SuperInner<!>) {
        return
    }
}
