// LANGUAGE: +FullValueClasses
// WITH_STDLIB

abstract value class OpenMod10 {
    abstract val x: Int
    override fun equals(other: Any?): Boolean = other is OpenMod10 && x % 10 == other.x % 10
    override fun hashCode(): Int = x % 10
}

abstract value class FinalMod10 {
    abstract val x: Int
    final override fun equals(other: Any?): Boolean = other is FinalMod10 && x % 10 == other.x % 10
    final override fun hashCode(): Int = x % 10
}

value class OpenSub(override val x: Int) : OpenMod10()
value class OpenMulti(override val x: Int, val y: Int) : OpenMod10()
value class FinalSub(override val x: Int) : FinalMod10()
value class FinalMulti(override val x: Int, val y: Int) : FinalMod10()

value class Modulo(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Modulo && x % 10 == other.x % 10
    override fun hashCode(): Int = x % 10
}

value class OpenSubOuter(val s: OpenSub)
value class FinalSubOuter(val s: FinalSub)
value class FinalMod10Outer(val m: FinalMod10)
value class ModuloOuter(val m: Modulo)
value class MultiOfOpenSub(val s: OpenSub, val y: Int)
value class MultiOfFinalSub(val s: FinalSub, val y: Int)
value class MultiOfFinalMod10(val m: FinalMod10, val y: Int)
value class MultiOfModulo(val m: Modulo, val y: Int)
value class MultiOfModuloOuter(val o: ModuloOuter, val y: Int)
value class OuterOfMulti(val m: MultiOfModuloOuter)
data class Holder(val o: FinalSubOuter, val m: MultiOfModuloOuter)

fun box(): String {
    // An open equals of a superclass is overridden by the generated one, like in data classes.
    if (OpenSub(1) == OpenSub(11)) return "Fail 1"
    if (OpenMulti(1, 5) == OpenMulti(11, 5)) return "Fail 2"
    if (OpenSubOuter(OpenSub(1)) == OpenSubOuter(OpenSub(11))) return "Fail 3"
    if (MultiOfOpenSub(OpenSub(1), 2) == MultiOfOpenSub(OpenSub(11), 2)) return "Fail 4"
    if (OpenSub(1) != OpenSub(1)) return "Fail 5"

    // A final equals of a superclass is inherited.
    if (FinalSub(1) != FinalSub(11)) return "Fail 6"
    if (FinalMulti(1, 5) != FinalMulti(11, 6)) return "Fail 7"
    if (FinalSubOuter(FinalSub(1)) != FinalSubOuter(FinalSub(11))) return "Fail 8"
    if (FinalMod10Outer(FinalSub(1)) != FinalMod10Outer(FinalMulti(21, 7))) return "Fail 9"
    if (MultiOfFinalSub(FinalSub(1), 2) != MultiOfFinalSub(FinalSub(11), 2)) return "Fail 10"
    if (MultiOfFinalMod10(FinalSub(1), 2) != MultiOfFinalMod10(FinalMulti(21, 7), 2)) return "Fail 11"
    if (MultiOfFinalSub(FinalSub(1), 2) == MultiOfFinalSub(FinalSub(11), 3)) return "Fail 12"

    // Fields of multi-field value classes are compared with their own equals.
    if (MultiOfModulo(Modulo(1), 2) != MultiOfModulo(Modulo(11), 2)) return "Fail 13"
    if (MultiOfModulo(Modulo(1), 2) == MultiOfModulo(Modulo(11), 3)) return "Fail 14"
    if (MultiOfModuloOuter(ModuloOuter(Modulo(1)), 2) != MultiOfModuloOuter(ModuloOuter(Modulo(11)), 2)) return "Fail 15"
    if (OuterOfMulti(MultiOfModuloOuter(ModuloOuter(Modulo(1)), 2)) != OuterOfMulti(MultiOfModuloOuter(ModuloOuter(Modulo(11)), 2))) {
        return "Fail 16"
    }

    val holder1 = Holder(FinalSubOuter(FinalSub(1)), MultiOfModuloOuter(ModuloOuter(Modulo(1)), 2))
    val holder2 = Holder(FinalSubOuter(FinalSub(11)), MultiOfModuloOuter(ModuloOuter(Modulo(11)), 2))
    if (holder1 != holder2 || holder1.hashCode() != holder2.hashCode()) return "Fail 17"
    if (setOf(holder1, holder2).size != 1) return "Fail 18"
    return "OK"
}
