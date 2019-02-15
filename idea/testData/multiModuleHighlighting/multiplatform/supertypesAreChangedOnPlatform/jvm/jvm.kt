interface Additional

actual interface Base : Additional // now Derived also inherits Additional

fun consumeAdditional(a: Additional) {}
fun consumePlatformBase(base: Base) {}
fun consumePlatformDerived(derived: Derived) {}

fun getAdditional(): Additional = null!!
fun getPlatformBase(): Base = null!!
fun getPlatformDerived(): Derived = null!!

fun commonToPlatformSubtyping() {
    // Base[Common] <: Additional
    consumeAdditional(getCommonBase())

    // Derived[Common] <: Additional
    consumeAdditional(getCommonDerived())

    // Derived[Common] <: Base[Platform]
    consumePlatformBase(getCommonDerived())
}

fun platformToCommonSubtyping() {
    // Base[Platform] <: Base[Common]
    consumeCommonBase(getPlatformBase())

    // Derived[Platform] <: Base[Common]
    consumeCommonBase(getPlatformDerived())
}

