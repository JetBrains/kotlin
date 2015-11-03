class Outer<E : Any> {
    inner class Inner<F, G>
}

val x: Outer<<!UPPER_BOUND_VIOLATED!>String?<!>>.Inner<String, Int> = null!!
