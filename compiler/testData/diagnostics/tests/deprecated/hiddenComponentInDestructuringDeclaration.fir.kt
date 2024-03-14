// TARGET_FRONTEND: ClassicFrontend
// ^ reason for a FIR mute: KT-66595

class SimpleKlass {
    @Deprecated("deprecated and hidden", level = DeprecationLevel.HIDDEN)
    operator fun component1(): Int = 42
}

fun test(simpleKlass: SimpleKlass) {
    val (s1) = simpleKlass
}
