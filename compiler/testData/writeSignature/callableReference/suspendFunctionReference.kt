interface I

abstract class AbstractTest {
    abstract fun normal(): suspend (String) -> Unit

    abstract fun extension(): suspend Double.() -> Double

    abstract fun bigArity(): suspend (I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I, I) -> Unit
}

class Test : AbstractTest() {
    override fun normal() = ::suspendNormal

    override fun extension() = Double::suspendExtension

    override fun bigArity() = this::suspendBigArity

    private suspend fun suspendNormal(s: String) {}

    private suspend fun suspendBigArity(
        p01: I, p02: I, p03: I, p04: I, p05: I, p06: I, p07: I, p08: I, p09: I, p10: I,
        p11: I, p12: I, p13: I, p14: I, p15: I, p16: I, p17: I, p18: I, p19: I, p20: I,
        p21: I, p22: I, p23: I, p24: I, p25: I, p26: I, p27: I, p28: I, p29: I, p30: I
    ) {}
}

private suspend fun Double.suspendExtension(): Double = this

// method: Test::normal
// jvm signature:     ()Lkotlin/reflect/KFunction;
// generic signature: ()Lkotlin/reflect/KFunction<Lkotlin/Unit;>;

// method: Test::extension
// jvm signature:     ()Lkotlin/reflect/KFunction;
// generic signature: ()Lkotlin/reflect/KFunction<Ljava/lang/Double;>;

// method: Test::bigArity
// jvm signature:     ()Lkotlin/reflect/KFunction;
// generic signature: ()Lkotlin/reflect/KFunction<Lkotlin/Unit;>;
