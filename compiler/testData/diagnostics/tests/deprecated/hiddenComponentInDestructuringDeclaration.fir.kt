class SimpleKlass {
    @Deprecated("deprecated and hidden", level = DeprecationLevel.HIDDEN)
    operator fun component1(): Int = 42
}

fun test(simpleKlass: SimpleKlass) {
    val (<!DEPRECATION_ERROR!>s1<!>) = simpleKlass
}
