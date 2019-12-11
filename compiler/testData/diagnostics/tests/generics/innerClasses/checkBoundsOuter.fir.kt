class Outer<E : Any> {
    inner class Inner<F, G>
}

val x: Outer<String?>.Inner<String, Int> = null!!
