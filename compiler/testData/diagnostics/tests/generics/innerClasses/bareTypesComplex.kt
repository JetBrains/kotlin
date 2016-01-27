open class SuperOuter<E> {
    inner open class SuperInner<F>
}

class DerivedOuter<G> : SuperOuter<G>() {
    inner class DerivedInner<H> : SuperOuter<G>.SuperInner<H>()
}

fun bare(x: SuperOuter<*>.SuperInner<*>, y: Any?) {
    if (x is SuperOuter.SuperInner) return
    if (y is <!NO_TYPE_ARGUMENTS_ON_RHS!>SuperOuter.SuperInner<!>) {
        return
    }
}
